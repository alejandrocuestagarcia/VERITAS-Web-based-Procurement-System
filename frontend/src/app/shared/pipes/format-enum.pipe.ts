import {Pipe, PipeTransform} from '@angular/core';

@Pipe({
  name: 'formatEnum'
})
export class FormatEnumPipe implements PipeTransform {

  transform(value: string, ...args: unknown[]): string {
    if (value == "") return "";

    const lower = value.replace(/_/g, ' ').toLowerCase();
    return lower.charAt(0).toUpperCase() + lower.slice(1);
  }

}
