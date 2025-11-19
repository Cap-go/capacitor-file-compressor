import { registerPlugin } from '@capacitor/core';

import type { FileCompressorPlugin } from './definitions';

const FileCompressor = registerPlugin<FileCompressorPlugin>('FileCompressor', {
  web: () => import('./web').then((m) => new m.FileCompressorWeb()),
});

export * from './definitions';
export { FileCompressor };
