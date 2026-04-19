# SocializerAI Demo Tunnel Guide (No Full Hosting)

Use this for quick public demo links without deploying to a cloud server.

## Prerequisite

Install Cloudflared once:

```powershell
winget install Cloudflare.cloudflared
```

## Start Demo URL

From project root:

```bat
demo-start.bat
```

What it does:
- Starts Docker services with `docker compose up -d`
- Starts a Cloudflare tunnel to `http://localhost:3000`
- Prints a public `https://...trycloudflare.com` URL

## Stop Demo URL

Stop only public tunnel:

```bat
demo-stop.bat
```

Stop tunnel and all containers:

```bat
demo-stop.bat --all
```

## Notes

- The URL is temporary and changes when tunnel restarts.
- Keep secrets out of public demos.
- If URL is not shown immediately, check `.demo\\cloudflared.log`.
