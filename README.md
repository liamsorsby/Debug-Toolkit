# Debug Toolkit

Debug Toolkit is a free Android app for checking your network connection and
troubleshooting connectivity problems. It puts common network tools in one place,
with clear results.

[Get it on Google Play](https://play.google.com/store/apps/details?id=co.sorsby.debugtoolkit)

Requires Android 10 or later. Available in English, Chinese (Simplified and
Traditional), Danish, Dutch, French, German, Italian, Japanese, Korean, Polish, Portuguese
(Brazil), Spanish, and Turkish.

## What you can do

**Connectivity**

- **Network** – see your connection type, whether it is metered, your local
  addresses, link speed estimates, and Wi-Fi signal strength, updated live.
- **Speed test** – measure latency, jitter, download, and upload speeds.
- **Ping and traceroute** – check whether a host is reachable and see the route
  your traffic takes.
- **Public IP and location** – see your address as the internet sees it.
- **CIDR calculator** – work out subnet ranges, masks, broadcast addresses, and
  usable hosts for IPv4 and IPv6.

**Security**

- **Certificate inspector** – check a website's TLS version, cipher, and
  certificate chain, including expiry dates.
- **HTTP inspector** – view a web address's status code, redirects, headers,
  timing, and a short preview of the response.

**Discovery**

- **DNS lookup** – look up A, AAAA, CNAME, MX, NS, TXT, SOA, CAA, SRV, and PTR
  records.
- **WHOIS lookup** – see a domain's registrar and expiry details.
- **Port scanner** – check which TCP ports are open on a host.
- **Local network scanner** – find devices on your current network.

Only scan hosts and networks you own or have permission to test.

## Good to know

- **Tools only run when you ask.** Nothing is scanned or tested in the background,
  and results are not saved.
- **Speed results are a snapshot.** They depend on your device, signal, route, and
  the nearest Cloudflare location, so they can vary between runs.
- **Wi-Fi signal strength is not distance.** It is a radio measurement in dBm, and
  walls, interference, and your device all affect it.
- **Some tools use Cloudflare.** The speed test and public IP tools connect to
  Cloudflare, as does DNS lookup unless you enter your own nameserver. Other tools
  connect only to the address you enter.

## Privacy

- **You choose whether to share usage analytics.** The app asks on first launch,
  and you can change your choice at any time in **Settings**. Declining keeps
  analytics and performance monitoring off.
- **Crash reports are always sent** so problems can be fixed. They never include
  what you type or your results.
- **Your inputs stay private.** Domains, web addresses, DNS answers, certificates,
  headers, local addresses, and Wi-Fi details are never sent to analytics.
- **Permissions:** network and Wi-Fi state are used to show connection details.
  Location (Android 12 and earlier) or Nearby Wi-Fi devices (Android 13 and later)
  is requested only to show Wi-Fi signal strength. The app never uses your location
  in the background.
- **The app is supported by ads.**

Read the full [privacy policy](https://debug-toolkit.sorsby.co/privacy-policy) and
[terms of use](https://debug-toolkit.sorsby.co/terms-of-use).

## Updates and feedback

Google Play may offer you an update inside the app. Very old versions eventually
stop working and ask you to update. After you have used the app for a while, you
might be invited to leave a review.

Found a bug or have an idea? [Open an issue](https://github.com/liamsorsby/Debug-Toolkit/issues).
To report a security problem, follow the [security policy](SECURITY.md) instead.

## Contributing

Developers can find build, testing, and release instructions in
[CONTRIBUTING.md](CONTRIBUTING.md).
