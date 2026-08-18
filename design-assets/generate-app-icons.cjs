const fs = require("node:fs");
const path = require("node:path");
const sharp = require("sharp");

const projectRoot = path.resolve(__dirname, "..");
const source = path.join(__dirname, "app-icon-v3-sleepcap-toy-source.png");
const masterOutput = path.join(__dirname, "app-icon-v3-sleepcap-toy-master.png");
const targets = {
  "mipmap-mdpi": 48,
  "mipmap-hdpi": 72,
  "mipmap-xhdpi": 96,
  "mipmap-xxhdpi": 144,
  "mipmap-xxxhdpi": 192,
};

function roundedMask(size) {
  const radius = Math.round(size * 0.22);
  return Buffer.from(
    `<svg width="${size}" height="${size}" xmlns="http://www.w3.org/2000/svg"><rect width="${size}" height="${size}" rx="${radius}" fill="white"/></svg>`,
  );
}

function renderIcon(size) {
  return sharp(source)
    .resize(size, size, { fit: "fill" })
    .composite([{ input: roundedMask(size), blend: "dest-in" }])
    .png({ compressionLevel: 9 });
}

async function generate() {
  await renderIcon(1024).toFile(masterOutput);

  for (const [density, size] of Object.entries(targets)) {
    const outputDirectory = path.join(projectRoot, "app", "src", "main", "res", density);
    fs.mkdirSync(outputDirectory, { recursive: true });
    await renderIcon(size).toFile(path.join(outputDirectory, "ic_launcher.png"));
  }
}

generate().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
