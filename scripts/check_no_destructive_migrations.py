#!/usr/bin/env python3
"""
Ensures fallbackToDestructiveMigration() is never used in the DayFlow project.
"""
import os
import sys

FORBIDDEN_STRING = "fallbackToDestructiveMigration("
EXCLUDE_DIRS = {".git", ".gradle", "build", "node_modules", "scripts"}

def check_files():
    found_violations = []
    root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

    for root, dirs, files in os.walk(root_dir):
        dirs[:] = [d for d in dirs if d not in EXCLUDE_DIRS]
        for file in files:
            if file.endswith((".kt", ".java", ".gradle", ".kts")):
                file_path = os.path.join(root, file)
                try:
                    with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                        for line_idx, line in enumerate(f, start=1):
                            if FORBIDDEN_STRING in line:
                                found_violations.append((file_path, line_idx, line.strip()))
                except Exception as e:
                    pass

    if found_violations:
        print(f"ERROR: Found {len(found_violations)} violation(s) of the zero-destructive-migration policy:")
        for path, line_no, content in found_violations:
            print(f"  {path}:{line_no} -> {content}")
        sys.exit(1)
    else:
        print("PASS: Zero instances of fallbackToDestructiveMigration found.")
        sys.exit(0)

if __name__ == "__main__":
    check_files()
