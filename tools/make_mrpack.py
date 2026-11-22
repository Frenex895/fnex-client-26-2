#!/usr/bin/env python3
"""Builds a Modrinth modpack (.mrpack) for FNEX CLIENT.

Usage:  python tools/make_mrpack.py build/libs/fnex-client-1.0.0.jar

The .mrpack contains: Fabric Loader + Minecraft version (dependencies),
Fabric API (downloaded from Modrinth, hash-verified) and the FNEX jar (as override).
Import it in the Modrinth App via  Add instance -> Import from file  (or drag & drop).
"""
import hashlib, json, os, sys, urllib.parse, urllib.request, zipfile

MC = "26.2"
LOADER = "0.19.5"
PACK_VERSION = "1.0.0"

def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "fnex-client-build/1.0"})
    return urllib.request.urlopen(req, timeout=30).read()

def main():
    if len(sys.argv) < 2 or not os.path.isfile(sys.argv[1]):
        sys.exit("Usage: make_mrpack.py <path to fnex-client-x.y.z.jar>")
    jar = sys.argv[1]

    q = urllib.parse.urlencode({"game_versions": json.dumps([MC]), "loaders": json.dumps(["fabric"])})
    versions = json.loads(get(f"https://api.modrinth.com/v2/project/fabric-api/version?{q}"))
    if not versions:
        sys.exit("No Fabric API version found for " + MC)
    f = next((x for x in versions[0]["files"] if x.get("primary")), versions[0]["files"][0])

    index = {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": PACK_VERSION,
        "name": "FNEX CLIENT",
        "summary": "FNEX CLIENT - premium PvP client (Fabric " + MC + ")",
        "files": [{
            "path": "mods/" + f["filename"],
            "hashes": {"sha1": f["hashes"]["sha1"], "sha512": f["hashes"]["sha512"]},
            "env": {"client": "required", "server": "unsupported"},
            "downloads": [f["url"]],
            "fileSize": f["size"],
        }],
        "dependencies": {"minecraft": MC, "fabric-loader": LOADER},
    }
    out = f"FNEX-CLIENT-26.2-{PACK_VERSION}.mrpack"
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("modrinth.index.json", json.dumps(index, indent=2))
        z.write(jar, "overrides/mods/" + os.path.basename(jar))
    print("Created", out)

if __name__ == "__main__":
    main()
