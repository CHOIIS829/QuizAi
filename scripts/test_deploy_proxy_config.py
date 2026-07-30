from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]


def read(path):
    return (ROOT / path).read_text()


class DeployProxyConfigTest(unittest.TestCase):
    def test_compose_has_stable_gateway_and_blue_green_apps(self):
        compose = read("docker-compose.yml")

        for service in (
            "gateway:",
            "frontend-blue:",
            "frontend-green:",
            "backend-blue:",
            "backend-green:",
        ):
            self.assertIn(service, compose)
        self.assertIn("./.deploy-state/nginx:/etc/nginx/runtime:ro", compose)

    def test_runtime_upstreams_default_to_blue(self):
        self.assertIn(
            "http://backend-blue:8080",
            read("deploy/nginx/backend-url.inc.example"),
        )
        self.assertIn(
            "http://frontend-blue:8080",
            read("deploy/nginx/frontend-url.inc.example"),
        )

    def test_deploy_switches_only_after_health_and_smoke_tests(self):
        deploy = read("deploy.sh")

        health_index = deploy.index('wait_for_backend "$TARGET_BACKEND_PORT"')
        smoke_index = deploy.index('run_ytdlp_smoke_test "$TARGET_BACKEND"')
        backend_switch_index = deploy.index(
            'write_upstream "$BACKEND_CONF" backend_url "$(service_url "$TARGET_BACKEND")"'
        )
        frontend_switch_index = deploy.index(
            'write_upstream "$FRONTEND_CONF" frontend_url "$(service_url "$TARGET_FRONTEND")"'
        )

        self.assertLess(health_index, smoke_index)
        self.assertLess(smoke_index, backend_switch_index)
        self.assertLess(smoke_index, frontend_switch_index)

    def test_deploy_uses_requested_ytdlp_smoke_video(self):
        deploy = read("deploy.sh")

        self.assertIn("https://youtu.be/CoyQM_Zi0OM", deploy)
        self.assertIn("--simulate", deploy)
        self.assertIn("YTDLP_SMOKE_TEST_RETRIES", deploy)

    def test_deploy_has_lock_rollback_and_graceful_stop(self):
        deploy = read("deploy.sh")

        self.assertIn("deploy.lock", deploy)
        self.assertIn("rollback_runtime_config", deploy)
        self.assertIn('= "rollback"', deploy)
        self.assertIn("BACKEND_STOP_TIMEOUT_SECONDS", deploy)

    def test_gateway_preserves_public_scheme_and_port(self):
        nginx = read("gateway/nginx/default.conf")

        self.assertIn("map $http_x_forwarded_proto $proxy_forwarded_proto", nginx)
        self.assertIn("proxy_set_header X-Forwarded-Proto $proxy_forwarded_proto;", nginx)
        self.assertIn("proxy_set_header X-Forwarded-Port $proxy_forwarded_port;", nginx)
        self.assertNotIn("proxy_set_header X-Forwarded-Proto $scheme;", nginx)

    def test_gateway_routes_frontend_and_backend_independently(self):
        nginx = read("gateway/nginx/default.conf")

        self.assertIn("proxy_pass $backend_url$request_uri;", nginx)
        self.assertIn("proxy_pass $frontend_url$request_uri;", nginx)
        self.assertIn("/etc/nginx/runtime/backend-url.inc", nginx)
        self.assertIn("/etc/nginx/runtime/frontend-url.inc", nginx)

    def test_prod_uses_validate_flyway_and_graceful_shutdown(self):
        prod = read("backend/src/main/resources/application-prod.yml")

        self.assertIn("ddl-auto: validate", prod)
        self.assertIn("baseline-on-migrate: true", prod)
        self.assertIn("enabled: true", prod)
        self.assertIn("shutdown: graceful", prod)
        self.assertIn("forward-headers-strategy: framework", prod)

    def test_prod_jwt_is_required_and_kakao_is_not_required_by_compose(self):
        compose = read("docker-compose.yml")
        prod = read("backend/src/main/resources/application-prod.yml")

        self.assertIn("JWT_SECRET_KEY: ${JWT_SECRET_KEY:?JWT_SECRET_KEY is required}", compose)
        self.assertNotIn("KAKAO_CLIENT_ID:?", compose)
        self.assertNotIn("KAKAO_CLIENT_SECRET:?", compose)
        self.assertIn("jwt-secret: ${JWT_SECRET_KEY}", prod)

    def test_redis_uses_aof(self):
        compose = read("docker-compose.yml")
        deploy = read("deploy.sh")

        self.assertIn("--appendonly yes", compose)
        self.assertIn("--appendfsync everysec", compose)
        self.assertIn("compose up -d --no-recreate db redis", deploy)

    def test_prometheus_is_not_exposed(self):
        application = read("backend/src/main/resources/application.yml")
        build = read("backend/build.gradle")

        self.assertNotIn("prometheus", application.lower())
        self.assertNotIn("micrometer-registry-prometheus", build)

    def test_deployment_images_support_amd64_and_arm64(self):
        for workflow_path in (
            ".github/workflows/deploy-home.yml",
            ".github/workflows/deploy-cloud.yml",
        ):
            workflow = read(workflow_path)

            self.assertIn("docker/setup-qemu-action@v3", workflow)
            self.assertEqual(
                2,
                workflow.count("platforms: linux/amd64,linux/arm64"),
                f"{workflow_path} must build both application images for amd64 and arm64",
            )

    def test_backend_installs_ytdlp_with_supported_python(self):
        dockerfile = read("backend/Dockerfile")

        self.assertIn("FROM eclipse-temurin:17-jre-noble", dockerfile)
        self.assertIn("python3-venv", dockerfile)
        self.assertIn("python3 -m venv /opt/yt-dlp", dockerfile)
        self.assertIn(
            "/opt/yt-dlp/bin/python -m pip install --no-cache-dir -U yt-dlp",
            dockerfile,
        )
        self.assertIn('ENV PATH="/opt/yt-dlp/bin:${PATH}"', dockerfile)
        self.assertNotIn("python3-pip", dockerfile)

    def test_prod_oauth_redirect_uri_uses_public_origin(self):
        prod = read("backend/src/main/resources/application-prod.yml")
        redirect_uri = (
            "${OAUTH2_REDIRECT_BASE_URL:https://quizai.co.kr}"
            "/login/oauth2/code/{registrationId}"
        )

        self.assertIn(f'redirect-uri: "{redirect_uri}"', prod)
        self.assertNotIn("quizai.co.kr:8080", prod)


if __name__ == "__main__":
    unittest.main()
