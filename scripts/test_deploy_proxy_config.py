from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]


def read(path):
    return (ROOT / path).read_text()


class DeployProxyConfigTest(unittest.TestCase):
    def test_frontend_mounts_runtime_service_url_config(self):
        compose = read("docker-compose.yml")

        self.assertIn(
            "./deploy/nginx/service-url.inc:/etc/nginx/conf.d/service-url.inc:ro",
            compose,
        )

    def test_deploy_updates_runtime_config_before_frontend_starts(self):
        deploy = read("deploy.sh")

        write_index = deploy.index('write_upstream "$TARGET_UPSTREAM"')
        frontend_index = deploy.index('compose up -d --build --no-deps frontend')

        self.assertLess(write_index, frontend_index)

    def test_deploy_uses_compose_service_dns_for_upstreams(self):
        deploy = read("deploy.sh")

        self.assertIn('TARGET_UPSTREAM="http://backend-green:8080"', deploy)
        self.assertIn('TARGET_UPSTREAM="http://backend-blue:8080"', deploy)
        self.assertNotIn('TARGET_UPSTREAM="http://quizAi-backend', deploy)

    def test_nginx_preserves_forwarded_https_proto(self):
        nginx = read("frontend/nginx/default.conf")

        self.assertIn("map $http_x_forwarded_proto $proxy_x_forwarded_proto", nginx)
        self.assertIn("proxy_set_header X-Forwarded-Proto $proxy_x_forwarded_proto;", nginx)
        self.assertNotIn("proxy_set_header X-Forwarded-Proto $scheme;", nginx)

    def test_prod_backend_uses_forward_headers(self):
        prod = read("backend/src/main/resources/application-prod.yml")

        self.assertIn("forward-headers-strategy: framework", prod)


if __name__ == "__main__":
    unittest.main()
