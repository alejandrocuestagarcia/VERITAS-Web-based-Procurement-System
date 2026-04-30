import CustomRenderer from './CustomRenderer';
import CustomPaletteProvider from './CustomPaletteProvider';
import CustomContextPadProvider from './CustomContextPadProvider';

export const viewerModules = {
  __init__: ['customRenderer'],
  customRenderer: ['type', CustomRenderer]
};

export const editorModules = {
  __init__: ['customRenderer', 'paletteProvider', 'contextPadProvider'],
  customRenderer: ['type', CustomRenderer],
  paletteProvider: ['type', CustomPaletteProvider],
  contextPadProvider: ['type', CustomContextPadProvider]
};
