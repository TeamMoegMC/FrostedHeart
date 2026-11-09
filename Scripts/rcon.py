"""Minimal Minecraft RCON client for driving a dev server from scripts.

Usage:
    python3 Scripts/rcon.py run/server.properties "list" "forge tps" "stop"

Reads server-ip, rcon.port and rcon.password from the given server.properties, so the password
never appears on the command line. Requires enable-rcon=true in that file. Standard library only.
"""
import socket
import struct
import sys

AUTH, COMMAND, END_MARKER = 3, 2, 0


def read_props(path):
    props = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                props[key] = value
    return props


def send(sock, req_id, kind, body):
    payload = struct.pack("<ii", req_id, kind) + body.encode("utf-8") + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(payload)) + payload)


def recv_exact(sock, n):
    buf = b""
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise ConnectionError("RCON connection closed")
        buf += chunk
    return buf


def recv(sock):
    (length,) = struct.unpack("<i", recv_exact(sock, 4))
    data = recv_exact(sock, length)
    req_id, kind = struct.unpack("<ii", data[:8])
    return req_id, kind, data[8:-2].decode("utf-8", "replace")


def run(sock, req_id, command):
    # Large outputs arrive split into 4096-byte packets; an unknown-type packet marks the end.
    # Vanilla RconClient requires exactly one packet per socket read, so the end marker is sent
    # only after the first reply arrives (the command packet has been consumed by then).
    send(sock, req_id, COMMAND, command)
    _, _, first = recv(sock)
    send(sock, req_id + 1, END_MARKER, "")
    parts = [first]
    while True:
        rid, _, body = recv(sock)
        if rid == req_id + 1:
            return "".join(parts)
        parts.append(body)


def main():
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    props = read_props(sys.argv[1])
    if props.get("enable-rcon") != "true":
        sys.exit(f"enable-rcon is not true in {sys.argv[1]}")
    host = props.get("server-ip") or "127.0.0.1"
    port = int(props.get("rcon.port", "25575"))
    with socket.create_connection((host, port), timeout=30) as sock:
        send(sock, 1, AUTH, props.get("rcon.password", ""))
        rid, _, _ = recv(sock)
        if rid == -1:
            sys.exit("RCON authentication failed")
        for i, command in enumerate(sys.argv[2:]):
            print(f"> {command}")
            print(run(sock, 10 + 2 * i, command) or "(no output)")


if __name__ == "__main__":
    main()
