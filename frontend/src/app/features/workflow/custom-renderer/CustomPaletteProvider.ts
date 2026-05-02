export default class CustomPaletteProvider {
  private palette: any;
  private create: any;
  private elementFactory: any;
  private lassoTool: any;
  private handTool: any;
  private globalConnect: any;

  constructor(
    palette: any,
    create: any,
    elementFactory: any,
    lassoTool: any,
    handTool: any,
    globalConnect: any
  ) {
    this.palette = palette;
    this.create = create;
    this.elementFactory = elementFactory;
    this.lassoTool = lassoTool;
    this.handTool = handTool;
    this.globalConnect = globalConnect;

    palette.registerProvider(this);
  }

  getPaletteEntries(): Record<string, any> {
    const { create, elementFactory, lassoTool, handTool, globalConnect } = this;

    function startDrag(type: string) {
      return (event: any) => {
        const shape = elementFactory.createShape({ type });
        create.start(event, shape);
      };
    }

    return {
      'hand-tool': {
        group: 'tools',
        className: 'bpmn-icon-hand-tool',
        title: 'Pan Canvas',
        action: {
          click: (event: any) => handTool.activateHand(event),
        },
      },
      'lasso-tool': {
        group: 'tools',
        className: 'bpmn-icon-lasso-tool',
        title: 'Select Multiple Elements',
        action: {
          click: (event: any) => lassoTool.activateSelection(event),
        },
      },
      'global-connect-tool': {
        group: 'tools',
        className: 'bpmn-icon-connection-multi',
        title: 'Draw Connection',
        action: {
          click: (event: any) => globalConnect.start(event),
        },
      },

      'tools-separator': { group: 'tools', separator: true },

      'create.start-event': {
        group: 'elements',
        className: 'bpmn-icon-start-event-none',
        title: 'Start Event',
        action: {
          dragstart: startDrag('bpmn:StartEvent'),
          click: startDrag('bpmn:StartEvent'),
        },
      },
      'create.exclusive-gateway': {
        group: 'elements',
        className: 'bpmn-icon-gateway-xor',
        title: 'Exclusive Gateway',
        action: {
          dragstart: startDrag('bpmn:ExclusiveGateway'),
          click: startDrag('bpmn:ExclusiveGateway'),
        },
      },
      'create.task': {
        group: 'elements',
        className: 'bpmn-icon-task',
        title: 'Task',
        action: {
          dragstart: startDrag('bpmn:Task'),
          click: startDrag('bpmn:Task'),
        },
      },
      'create.end-event': {
        group: 'elements',
        className: 'bpmn-icon-end-event-none',
        title: 'End Event',
        action: {
          dragstart: startDrag('bpmn:EndEvent'),
          click: startDrag('bpmn:EndEvent'),
        },
      },

      'elements-separator': { group: 'elements', separator: true },

      'create.text-annotation': {
        group: 'elements',
        className: 'bpmn-icon-text-annotation',
        title: 'Text Annotation',
        action: {
          dragstart: startDrag('bpmn:TextAnnotation'),
          click: startDrag('bpmn:TextAnnotation'),
        },
      },
    };
  }
}

(CustomPaletteProvider as any).$inject = [
  'palette',
  'create',
  'elementFactory',
  'lassoTool',
  'handTool',
  'globalConnect',
];
