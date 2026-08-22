const sharp = require("sharp");
const path = require("path");

const root = path.resolve(__dirname);
const reference = path.join(root, "journal-v1.3.8-full-screen-reference.png");
const actual = path.join(root, "qa", "journal-v1.3.8-deviation-xiaomi14pro.png");
const referenceCrop = path.join(root, "qa", "journal-v1.3.8-deviation-reference-crop.png");
const comparison = path.join(root, "qa", "journal-v1.3.8-deviation-reference-comparison.png");

async function main() {
  await sharp(reference)
    .extract({ left: 22, top: 882, width: 808, height: 392 })
    .png()
    .toFile(referenceCrop);

  const actualBuffer = await sharp(actual)
    .resize({ width: 808 })
    .png()
    .toBuffer();
  const actualMetadata = await sharp(actualBuffer).metadata();
  const referenceBuffer = await sharp(referenceCrop).png().toBuffer();
  const canvasHeight = Math.max(392, actualMetadata.height || 0);

  await sharp({
    create: {
      width: 808 * 2 + 24,
      height: canvasHeight,
      channels: 4,
      background: { r: 255, g: 244, b: 214, alpha: 1 }
    }
  })
    .composite([
      { input: referenceBuffer, left: 0, top: 0 },
      { input: actualBuffer, left: 832, top: 0 }
    ])
    .png()
    .toFile(comparison);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
