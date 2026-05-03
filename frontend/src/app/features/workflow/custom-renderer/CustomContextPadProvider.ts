import { is } from 'bpmn-js/lib/util/ModelUtil';

export default class CustomContextPadProvider {
  private contextPad: any;
  private modeling: any;
  private elementFactory: any;
  private connect: any;
  private autoPlace: any;
  private create: any;

  constructor(
    contextPad: any,
    modeling: any,
    elementFactory: any,
    connect: any,
    autoPlace: any,
    create: any
  ) {
    this.contextPad = contextPad;
    this.modeling = modeling;
    this.elementFactory = elementFactory;
    this.connect = connect;
    this.autoPlace = autoPlace;
    this.create = create;

    contextPad.registerProvider(this);
  }

  getContextPadEntries(element: any): Record<string, any> {
    const { modeling, elementFactory, connect, autoPlace, create } = this;

    function deleteElement() {
      modeling.removeElements([element]);
    }

    const deleteEntry = {
      group: 'edit',
      className: 'bpmn-icon-trash',
      title: 'Delete',
      action: { click: deleteElement },
    };
    const textAnnotationEntry = {
      group: 'edit',
      className: 'bpmn-icon-text-annotation',
      title: 'Add Text Annotation',
      action: {
        click: appendTextAnnotation,
        dragstart: startAppendTextAnnotation,
      },
    };
    const connectEntry = {
      group: 'connect',
      className: 'bpmn-icon-connection-multi',
      title: 'Connect to Element',
      action: { click: startConnect, dragstart: startConnect },
    };

    if (is(element, 'bpmn:SequenceFlow')) {
      return {
        textAnnotationEntry,
        delete: deleteEntry
      };
    }

    function startConnect(event: any, el: any) {
      connect.start(event, el);
    }

    function appendTextAnnotation(_event: any, el: any) {
      const shape = elementFactory.createShape({ type: 'bpmn:TextAnnotation' });
      autoPlace.append(el, shape);
    }

    function startAppendTextAnnotation(event: any, el: any) {
      const shape = elementFactory.createShape({ type: 'bpmn:TextAnnotation' });
      create.start(event, shape, { source: el });
    }

    return {
      textAnnotationEntry,
      connect: connectEntry,
      delete: deleteEntry,
    };
  }
}

(CustomContextPadProvider as any).$inject = [
  'contextPad',
  'modeling',
  'elementFactory',
  'connect',
  'autoPlace',
  'create',
];
