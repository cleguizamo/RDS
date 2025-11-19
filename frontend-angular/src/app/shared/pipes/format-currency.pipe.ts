import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'formatCurrency',
  standalone: true
})
export class FormatCurrencyPipe implements PipeTransform {
  transform(value: number | null | undefined, decimals: number = 1): string {
    if (value == null || value === 0) {
      return '$0';
    }

    const absValue = Math.abs(value);
    const sign = value >= 0 ? '' : '-';
    
    // Millones
    if (absValue >= 1000000) {
      return sign + '$' + (absValue / 1000000).toFixed(decimals) + 'M';
    }
    
    // Miles
    if (absValue >= 1000) {
      return sign + '$' + (absValue / 1000).toFixed(decimals) + 'K';
    }
    
    // Valor menor a 1000, mostrar normal con 2 decimales
    return sign + '$' + absValue.toFixed(2);
  }
}

