"""Send commands to a dev client started with Scripts/dev_client_bridge.init.gradle.

Usage:
    python3 Scripts/agent_client.py [--dir run/agent] [--timeout SECONDS] "<command>" ["<command>" ...]

Commands run one at a time, in order; the script prints each JSON result and stops at the first failure.
Commands may be queued before the client has finished starting. GUI coordinates are the scaled GUI
coordinates reported by 'screen' (multiply by guiScale for screenshot pixels).

    state                          player, screen, position, looked-at block
    connect <host[:port]>          join a server from the title screen
    world <save folder>            open an existing singleplayer save
    newworld <folder> [flat|normal] [seed]   create a creative, peaceful world with cheats
    disconnect                     leave the world and return to the title screen
    command /<command>             run a command as the player; returns chat replies from the next second
    command <text>                 send a chat message
    messages                       recent chat and system messages
    look <yaw> <pitch>             set the view direction
    lookat <x> <y> <z>             look at the centre of a block
    key <name>                     press a key: e, escape, key.mouse.right (use the looked-at block), ...
    type <text>                    type text into the open screen
    click <x> <y> [left|right|middle]       click inside the open screen
    scroll <x> <y> <delta>         scroll inside the open screen
    move <x> <y>                   move the in-game cursor (hover/tooltips) without moving the OS cursor
    screen                         describe the open screen: vanilla widgets, container slots, CUI element tree
    screenshot [name]              save a PNG of the next frame and return its path
    wait <ticks>                   wait for game ticks
    quit                           close the client
"""
import argparse
import json
import os
import sys
import time


def send(directory, command, timeout):
    inbox = os.path.join(directory, "inbox")
    outbox = os.path.join(directory, "outbox")
    os.makedirs(inbox, exist_ok=True)
    os.makedirs(outbox, exist_ok=True)
    job_id = f"{time.time_ns():020d}"
    temp = os.path.join(inbox, job_id + ".tmp")
    with open(temp, "w", encoding="utf-8") as f:
        f.write(command)
    os.replace(temp, os.path.join(inbox, job_id + ".cmd"))
    result_path = os.path.join(outbox, job_id + ".json")
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if os.path.exists(result_path):
            with open(result_path, encoding="utf-8") as f:
                result = json.load(f)
            os.remove(result_path)
            return result
        time.sleep(0.1)
    return {"id": job_id, "command": command, "ok": False,
            "error": f"no reply within {timeout}s (is the client running with the bridge?)"}


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dir", default=os.path.join("run", "agent"), help="bridge directory (default: run/agent)")
    parser.add_argument("--timeout", type=float, default=600, help="seconds to wait for each reply (default: 600)")
    parser.add_argument("commands", nargs="+")
    options = parser.parse_args()
    for command in options.commands:
        result = send(options.dir, command, options.timeout)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        if not result.get("ok"):
            sys.exit(1)


if __name__ == "__main__":
    main()
