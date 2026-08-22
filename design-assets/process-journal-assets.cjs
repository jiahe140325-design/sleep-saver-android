const path = require("node:path");
const sharp = require("sharp");

const projectRoot = path.resolve(__dirname, "..");
const drawableDir = path.join(projectRoot, "app", "src", "main", "res", "drawable-nodpi");

const assets = [
  {
    source: "journal-sleepy-mascot-source.png",
    designOutput: "journal-sleepy-mascot-transparent.png",
    androidOutput: "journal_sleepy_mascot.png",
    width: 512,
  },
  {
    source: "journal-tape-gingham-source.png",
    designOutput: "journal-tape-gingham-transparent.png",
    androidOutput: "journal_tape_gingham.png",
    width: 512,
  },
  {
    source: "journal-tape-polka-source.png",
    designOutput: "journal-tape-polka-transparent.png",
    androidOutput: "journal_tape_polka.png",
    width: 512,
  },
  {
    source: "journal-plan-ribbon-source.png",
    designOutput: "journal-plan-ribbon-transparent.png",
    androidOutput: "journal_plan_ribbon.png",
    width: 1200,
  },
];

async function removeChromaGreen(sourcePath) {
  const { data, info } = await sharp(sourcePath)
    .ensureAlpha()
    .raw()
    .toBuffer({ resolveWithObject: true });

  for (let index = 0; index < data.length; index += info.channels) {
    const red = data[index];
    const green = data[index + 1];
    const blue = data[index + 2];
    const alpha = data[index + 3];
    const strongestNonGreen = Math.max(red, blue);
    const greenExcess = green - strongestNonGreen;

    if (green > 110 && greenExcess > 18) {
      const keyStrength = Math.min(1, (greenExcess - 18) / 105);
      data[index + 3] = Math.round(alpha * (1 - keyStrength));
      data[index + 1] = Math.min(green, Math.round((red + blue) / 2) + 10);
    }
  }

  return sharp(data, {
    raw: {
      width: info.width,
      height: info.height,
      channels: info.channels,
    },
  });
}

async function processAsset(asset) {
  const sourcePath = path.join(__dirname, asset.source);
  const designPath = path.join(__dirname, asset.designOutput);
  const androidPath = path.join(drawableDir, asset.androidOutput);
  const transparent = await removeChromaGreen(sourcePath);
  const buffer = await transparent
    .trim({ background: { r: 0, g: 0, b: 0, alpha: 0 }, threshold: 8 })
    .resize({ width: asset.width, withoutEnlargement: true })
    .png({ compressionLevel: 9 })
    .toBuffer();

  await sharp(buffer).toFile(designPath);
  await sharp(buffer).toFile(androidPath);
}

async function processTapeSheet() {
  const sourcePath = path.join(__dirname, "journal-duration-tapes-source.png");
  const transparent = await removeChromaGreen(sourcePath);
  const { data, info } = await transparent.ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  const occupiedColumns = Array.from({ length: info.width }, () => false);

  for (let x = 0; x < info.width; x += 1) {
    let visiblePixels = 0;
    for (let y = 0; y < info.height; y += 1) {
      const alpha = data[(y * info.width + x) * info.channels + 3];
      if (alpha > 28) visiblePixels += 1;
    }
    occupiedColumns[x] = visiblePixels > 12;
  }

  const runs = [];
  let runStart = null;
  occupiedColumns.forEach((occupied, index) => {
    if (occupied && runStart === null) runStart = index;
    if ((!occupied || index === occupiedColumns.length - 1) && runStart !== null) {
      const runEnd = occupied ? index : index - 1;
      if (runEnd - runStart > 40) runs.push([runStart, runEnd]);
      runStart = null;
    }
  });

  if (runs.length !== 7) {
    throw new Error(`Expected seven tape strips, found ${runs.length}`);
  }

  await Promise.all(runs.map(async ([left, right], index) => {
    let top = info.height;
    let bottom = 0;
    for (let x = left; x <= right; x += 1) {
      for (let y = 0; y < info.height; y += 1) {
        const alpha = data[(y * info.width + x) * info.channels + 3];
        if (alpha > 28) {
          top = Math.min(top, y);
          bottom = Math.max(bottom, y);
        }
      }
    }

    const padding = 6;
    const cropLeft = Math.max(0, left - padding);
    const cropTop = Math.max(0, top - padding);
    const cropWidth = Math.min(info.width - cropLeft, right - left + 1 + padding * 2);
    const cropHeight = Math.min(info.height - cropTop, bottom - top + 1 + padding * 2);
    const buffer = await sharp(data, {
      raw: { width: info.width, height: info.height, channels: info.channels },
    })
      .extract({ left: cropLeft, top: cropTop, width: cropWidth, height: cropHeight })
      .resize({ width: 160 })
      .png({ compressionLevel: 9 })
      .toBuffer();
    const number = index + 1;
    await sharp(buffer).toFile(path.join(__dirname, `journal-duration-tape-${number}.png`));
    await sharp(buffer).toFile(path.join(drawableDir, `journal_duration_tape_${number}.png`));
  }));
}

Promise.all([...assets.map(processAsset), processTapeSheet()]).catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
