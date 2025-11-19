import { FileCompressor } from '@capgo/capacitor-file-compressor';
import { Capacitor } from '@capacitor/core';

let selectedFile = null;

// Update quality display
const qualityInput = document.getElementById('quality');
const qualityValue = document.getElementById('qualityValue');
qualityInput.addEventListener('input', (e) => {
  qualityValue.textContent = e.target.value;
});

// Handle file selection
const fileInput = document.getElementById('fileInput');
const compressBtn = document.getElementById('compressBtn');
const errorDiv = document.getElementById('error');

fileInput.addEventListener('change', (e) => {
  const file = e.target.files[0];
  if (file) {
    selectedFile = file;
    compressBtn.disabled = false;
    errorDiv.style.display = 'none';

    // Show original image preview
    const originalImage = document.getElementById('originalImage');
    const originalInfo = document.getElementById('originalInfo');

    const reader = new FileReader();
    reader.onload = (event) => {
      originalImage.src = event.target.result;

      const img = new Image();
      img.onload = () => {
        originalInfo.innerHTML = `
          <strong>Size:</strong> ${formatFileSize(file.size)}<br>
          <strong>Dimensions:</strong> ${img.width} x ${img.height}px<br>
          <strong>Type:</strong> ${file.type}
        `;
      };
      img.src = event.target.result;
    };
    reader.readAsDataURL(file);
  } else {
    selectedFile = null;
    compressBtn.disabled = true;
  }
});

// Handle compression
compressBtn.addEventListener('click', async () => {
  if (!selectedFile) return;

  errorDiv.style.display = 'none';
  compressBtn.disabled = true;
  compressBtn.textContent = 'Compressing...';

  try {
    const quality = parseFloat(qualityInput.value);
    const width = document.getElementById('width').value;
    const height = document.getElementById('height').value;
    const mimeType = document.getElementById('mimeType').value;

    const options = {
      quality,
      mimeType,
    };

    // Add blob for web, path for native
    if (Capacitor.getPlatform() === 'web') {
      options.blob = selectedFile;
    } else {
      // On native platforms, you would get the path from a file picker
      // For this example, we'll show an error
      throw new Error('Native platform example requires file picker integration');
    }

    if (width) {
      options.width = parseInt(width);
    }
    if (height) {
      options.height = parseInt(height);
    }

    const result = await FileCompressor.compressImage(options);

    // Display compressed image
    const compressedImage = document.getElementById('compressedImage');
    const compressedInfo = document.getElementById('compressedInfo');
    const preview = document.getElementById('preview');

    if (result.blob) {
      const url = URL.createObjectURL(result.blob);
      compressedImage.src = url;

      const img = new Image();
      img.onload = () => {
        const compressionRatio = ((1 - result.blob.size / selectedFile.size) * 100).toFixed(1);
        compressedInfo.innerHTML = `
          <strong>Size:</strong> ${formatFileSize(result.blob.size)}<br>
          <strong>Dimensions:</strong> ${img.width} x ${img.height}px<br>
          <strong>Type:</strong> ${result.blob.type}<br>
          <strong>Compression:</strong> ${compressionRatio}% reduction
        `;
      };
      img.src = url;
    } else if (result.path) {
      compressedInfo.innerHTML = `<strong>Path:</strong> ${result.path}`;
    }

    preview.style.display = 'block';
  } catch (error) {
    errorDiv.textContent = `Error: ${error.message}`;
    errorDiv.style.display = 'block';
    console.error('Compression error:', error);
  } finally {
    compressBtn.disabled = false;
    compressBtn.textContent = 'Compress Image';
  }
});

function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}
