#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
project_root="$(cd "$script_dir/.." && pwd)"
source_image="$script_dir/journal-deviation-label-source.png"
design_output="$script_dir/journal-deviation-label-transparent.png"
android_output="$project_root/app/src/main/res/drawable-nodpi/journal_deviation_label.png"
ffmpeg_bin="/opt/homebrew/bin/ffmpeg"

if [[ ! -x "$ffmpeg_bin" ]]; then
  ffmpeg_bin="$(command -v ffmpeg)"
fi

"$ffmpeg_bin" -y \
  -i "$source_image" \
  -vf "colorkey=0x00ff00:0.34:0.10,despill=type=green:mix=1:expand=0.25:green=-1.2,crop=630:360:615:250,scale=512:-1" \
  -frames:v 1 \
  -update 1 \
  "$design_output"

cp "$design_output" "$android_output"
