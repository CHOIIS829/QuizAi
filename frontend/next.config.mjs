const isProd = process.env.NODE_ENV === 'production';

/** @type {import('next').NextConfig} */
const nextConfig = {
  /* config options here */
  reactCompiler: true,
  output: isProd ? 'export' : undefined,
  images: {
    unoptimized: true,
  },
};

if (!isProd) {
  nextConfig.rewrites = async () => ([
    {
      source: '/api/:path*',
      destination: 'http://localhost:8080/api/:path*',
    },
    {
      source: '/oauth2/:path*',
      destination: 'http://localhost:8080/oauth2/:path*',
    },
    {
      source: '/login/oauth2/:path*',
      destination: 'http://localhost:8080/login/oauth2/:path*',
    },
  ]);
}

export default nextConfig;
