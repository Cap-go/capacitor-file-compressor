import { WebPlugin } from '@capacitor/core';

import type { CompressImageOptions, CompressImageResult, FileCompressorPlugin } from './definitions';

export class FileCompressorWeb extends WebPlugin implements FileCompressorPlugin {
  private readonly minimumQuality = 0.1;
  private readonly qualityStep = 0.05;

  async compressImage(options: CompressImageOptions): Promise<CompressImageResult> {
    if (!options.blob) {
      throw new Error('blob is required on web platform');
    }

    const quality = options.quality ?? 0.6;
    const mimeType = options.mimeType ?? 'image/jpeg';

    if (mimeType !== 'image/jpeg' && mimeType !== 'image/webp') {
      throw new Error('Only image/jpeg and image/webp are supported on web platform');
    }

    const img = await this.createImageFromBlob(options.blob);
    const { width, height } = this.calculateDimensions(img, options.width, options.height);

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;

    const ctx = canvas.getContext('2d');
    if (!ctx) {
      throw new Error('Failed to get canvas context');
    }

    ctx.drawImage(img, 0, 0, width, height);

    const blob = await this.canvasToBlobWithinSize(canvas, mimeType, quality, options.blob.size);

    return { blob };
  }

  async getPluginVersion(): Promise<{ version: string }> {
    return { version: '7.0.0' };
  }

  private calculateDimensions(
    img: HTMLImageElement,
    maxWidth?: number,
    maxHeight?: number,
  ): { width: number; height: number } {
    let width = img.width;
    let height = img.height;

    if (maxWidth != null && maxHeight != null) {
      const ratio = Math.min(maxWidth / width, maxHeight / height, 1);
      width = Math.round(width * ratio);
      height = Math.round(height * ratio);
    } else if (maxWidth != null) {
      const ratio = Math.min(maxWidth / width, 1);
      width = Math.round(width * ratio);
      height = Math.round(height * ratio);
    } else if (maxHeight != null) {
      const ratio = Math.min(maxHeight / height, 1);
      width = Math.round(width * ratio);
      height = Math.round(height * ratio);
    }

    return { width, height };
  }

  private async canvasToBlobWithinSize(
    canvas: HTMLCanvasElement,
    mimeType: string,
    quality: number,
    maxBytes: number,
  ): Promise<Blob> {
    let currentQuality = quality;

    while (true) {
      const blob = await this.canvasToBlob(canvas, mimeType, currentQuality);
      if (blob.size <= maxBytes || currentQuality <= this.minimumQuality) {
        return blob;
      }

      currentQuality = Math.max(this.minimumQuality, currentQuality - this.qualityStep);
    }
  }

  private canvasToBlob(canvas: HTMLCanvasElement, mimeType: string, quality: number): Promise<Blob> {
    return new Promise((resolve, reject) => {
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
