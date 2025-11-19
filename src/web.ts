import { WebPlugin } from '@capacitor/core';

import type { CompressImageOptions, CompressImageResult, FileCompressorPlugin } from './definitions';

export class FileCompressorWeb extends WebPlugin implements FileCompressorPlugin {
  async compressImage(options: CompressImageOptions): Promise<CompressImageResult> {
    if (!options.blob) {
      throw new Error('blob is required on web platform');
    }

    const quality = options.quality ?? 0.6;
    const mimeType = options.mimeType ?? 'image/jpeg';

    // Validate mime type for web
    if (mimeType !== 'image/jpeg' && mimeType !== 'image/webp') {
      throw new Error('Only image/jpeg and image/webp are supported on web platform');
    }

    // Create image element to load the blob
    const img = await this.createImageFromBlob(options.blob);

    // Calculate dimensions
    let width = options.width ?? img.width;
    let height = options.height ?? img.height;

    // Maintain aspect ratio if only one dimension is provided
    if (options.width && !options.height) {
      height = (img.height / img.width) * width;
    } else if (options.height && !options.width) {
      width = (img.width / img.height) * height;
    }

    // Create canvas and draw image
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;

    const ctx = canvas.getContext('2d');
    if (!ctx) {
      throw new Error('Failed to get canvas context');
    }

    ctx.drawImage(img, 0, 0, width, height);

    // Convert canvas to blob
    const blob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (blob) => {
          if (blob) {
            resolve(blob);
          } else {
            reject(new Error('Failed to create blob from canvas'));
          }
        },
        mimeType,
        quality,
      );
    });

    return { blob };
  }

  async getPluginVersion(): Promise<{ version: string }> {
    return { version: '7.0.0' };
  }

  private createImageFromBlob(blob: Blob): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      const url = URL.createObjectURL(blob);

      img.onload = () => {
        URL.revokeObjectURL(url);
        resolve(img);
      };

      img.onerror = () => {
        URL.revokeObjectURL(url);
        reject(new Error('Failed to load image from blob'));
      };

      img.src = url;
    });
  }
}
