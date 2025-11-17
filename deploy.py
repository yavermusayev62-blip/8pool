#!/usr/bin/env python3

import argparse
import subprocess
import sys
from datetime import datetime


def run(cmd, **kwargs):
    result = subprocess.run(cmd, text=True, capture_output=True, **kwargs)
    if result.returncode != 0:
        raise RuntimeError(
            f"Command {' '.join(cmd)} failed:\n{result.stdout}\n{result.stderr}"
        )
    return result.stdout.strip()


def in_git_repo() -> bool:
    result = subprocess.run(
        ["git", "rev-parse", "--is-inside-work-tree"],
        text=True,
        capture_output=True,
    )
    return result.returncode == 0


def prompt_for_remote() -> str:
    try:
        entered = input(
            "Git remote URL daxil edin (məs: https://github.com/user/repo.git): "
        ).strip()
    except EOFError:
        entered = ""
    return entered


def ensure_repo(remote_url: str | None, branch: str | None):
    if in_git_repo():
        if remote_url:
            configure_remote(remote_url)
        return

    if not remote_url:
        remote_url = prompt_for_remote()
    if not remote_url:
        raise RuntimeError("Uzaq repo URL-i tələb olunur.")

    print("Git repozitoriyası tapılmadı. Yeni repo yaradılır...")
    run(["git", "init"])
    if branch:
        run(["git", "checkout", "-B", branch])
    configure_remote(remote_url)


def configure_remote(remote_url: str):
    remotes = run(["git", "remote"]).split()
    if "origin" in remotes:
        run(["git", "remote", "set-url", "origin", remote_url])
    else:
        run(["git", "remote", "add", "origin", remote_url])


def main():
    parser = argparse.ArgumentParser(
        description="Add, commit, and push the current repository changes."
    )
    parser.add_argument(
        "message",
        nargs="?",
        default=f"deploy {datetime.now():%Y-%m-%d %H:%M}",
        help="Commit message to use (default: timestamped message).",
    )
    parser.add_argument(
        "--remote",
        help="Git remote URL. Lazım gələrsə repo avtomatik yaradılıb bu URL-ə qoşulacaq.",
    )
    parser.add_argument(
        "--branch",
        help="Push ediləcək budaq adı. Yeni repo yaranarsa bu adla yaradılacaq.",
    )
    args = parser.parse_args()

    ensure_repo(args.remote, args.branch)

    status = run(["git", "status", "--short"])
    if not status:
        print("Gonderilecek dəyişiklik yoxdur. Çıxılır.")
        return

    run(["git", "add", "--all"])
    run(["git", "commit", "-m", args.message])
    branch = args.branch or run(["git", "rev-parse", "--abbrev-ref", "HEAD"])
    print(f"origin/{branch} uzaq budağına push edilir...")
    run(["git", "push", "origin", branch])
    print("Deploy əməliyyatı uğurla bitdi.")


if __name__ == "__main__":
    try:
        main()
    except RuntimeError as err:
        print(err, file=sys.stderr)
        sys.exit(1)

