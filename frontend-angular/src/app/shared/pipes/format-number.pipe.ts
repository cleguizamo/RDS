import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'formatNumber',
  standalone: true
})
export class FormatNumberPipe implements PipeTransform {
  transform(value: number | null | undefined, decimals: number = 1): string {
    if (value == null || value === 0) {
      return '0';
    }

    const absValue = Math.abs(value);
    
    // Millones
    if (absValue >= 1000000) {
      return (value >= 0 ? '' : '-') + (absValue / 1000000).toFixed(decimals) + 'M';
    }
    
    // Miles
    if (absValue >= 1000) {
      return (value >= 0 ? '' : '-') + (absValue / 1000).toFixed(decimals) + 'K';
    }
    
    // Valor menor a 1000, mostrar normal con decimales si es necesario
    return value.toFixed(decimals === 0 ? 0 : 2);
  }
}

