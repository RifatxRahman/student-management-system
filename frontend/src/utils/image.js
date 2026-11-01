const MAX_SIDE = 1024
const MAX_BYTES = 2 * 1024 * 1024

// বাছাই করা ছবিকে ছোট JPEG বানিয়ে ফেরায়। ছবি না হলে বা খুব বড় থাকলে Error ছোড়ে।
export async function shrinkImage(file) {
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    throw new Error('শুধু JPEG বা PNG ছবি দেওয়া যাবে')
  }

  let bitmap
  try {
    bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' })
  } catch {
    throw new Error('ছবিটা পড়া যায়নি')
  }

  const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height))
  const width = Math.max(1, Math.round(bitmap.width * scale))
  const height = Math.max(1, Math.round(bitmap.height * scale))

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  context.fillStyle = '#ffffff' // স্বচ্ছ (PNG) অংশ সাদা হবে
  context.fillRect(0, 0, width, height)
  context.drawImage(bitmap, 0, 0, width, height)
  bitmap.close()

  const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.85))
  if (!blob) {
    throw new Error('ছবি প্রক্রিয়া করা যায়নি')
  }
  if (blob.size > MAX_BYTES) {
    throw new Error('ছবিটা খুব বড়, ছোট মাপের ছবি বেছে নিন')
  }
  return blob
}