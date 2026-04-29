import BaseRenderer from 'diagram-js/lib/draw/BaseRenderer';
import { append as svgAppend, attr as svgAttr, create as svgCreate , remove as svgRemove } from 'tiny-svg';
import { getRoundRectPath } from 'bpmn-js/lib/draw/BpmnRenderUtil';
import { is } from 'bpmn-js/lib/util/ModelUtil';

const HIGH_PRIORITY = 1500;

export default class CustomRenderer extends BaseRenderer {
  private bpmnRenderer: any;
  constructor(eventBus: any, bpmnRenderer: any) {
    super(eventBus, HIGH_PRIORITY);
    this.bpmnRenderer = bpmnRenderer;
  }

  override canRender(element: any): boolean {
    return is(element, 'bpmn:Task') ||
      is(element, 'bpmn:StartEvent') ||
      is(element, 'bpmn:EndEvent') ||
      is(element, 'bpmn:Gateway');
  }

  override drawShape(parentNode: any, element: any) {
    const shape = this.bpmnRenderer.drawShape(parentNode, element);

    if (element.labelTarget || element.type === 'label') {
      svgAttr(shape, {
        fill: '#191c1e'
      });
      return shape;
    }

    if (is(element, 'bpmn:Task')) {
      svgAttr(shape, {
        stroke: '#003d9b',
        strokeWidth: 2,
        rx: 20,
        ry: 20,
        fill: '#f0f7ff',
        filter: 'drop-shadow(0px 4px 6px rgba(0, 0, 0, 0.05))'
      });
    }

    if (is(element, 'bpmn:Gateway')) {
      svgAttr(shape, {
        stroke: '#f59e0b',
        strokeWidth: 2,
        fill: '#fff7ed'
      });
    }

    if (is(element, 'bpmn:StartEvent')) {
      const rect = svgCreate('rect');
      svgAttr(rect, {
        width: element.width,
        height: element.height,
        rx: 10,
        ry: 10,
        stroke: '#10b981',
        strokeWidth: 2,
        fill: '#ecfdf5'
      });

      //AI-generated
      const playIcon = svgCreate('polygon');
      svgAttr(playIcon, {
        points: '13,10 25,18 13,26', // Coordinates relative to start event (36x36)
        fill: '#10b981'
      });
      //AI end

      svgAppend(parentNode, rect);
      svgAppend(parentNode, playIcon);

      svgRemove(shape);
      return rect;
    }

    if (is(element, 'bpmn:EndEvent')) {
      svgAttr(shape, {
        stroke: '#ef4444',
        strokeWidth: 2,
        fill: '#fef2f2'
      });
    }

    return shape;
  }

  override getShapePath(shape: any) {
    if (is(shape, 'bpmn:Task')) {
      return getRoundRectPath(shape, 12);
    }
    return this.bpmnRenderer.getShapePath(shape);
  }
}

(CustomRenderer as any).$inject = ['eventBus', 'bpmnRenderer'];
