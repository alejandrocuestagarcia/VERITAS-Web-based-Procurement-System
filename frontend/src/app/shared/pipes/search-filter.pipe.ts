import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'searchFilter'
})
export class SearchFilterPipe implements PipeTransform {
  transform(items: any[], searchText: string, fields: string): any[] {
    if (!items || !searchText || searchText.trim() === '') {
      return items;
    }
    const lowerSearch = searchText.toLowerCase();
    const fieldList = fields.split(',').map(f => f.trim());
    return items.filter(item =>
      fieldList.some(field => {
        const value = item[field];
        return value && String(value).toLowerCase().includes(lowerSearch);
      })
    );
  }
}
