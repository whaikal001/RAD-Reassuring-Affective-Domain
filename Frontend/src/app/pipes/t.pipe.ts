import { Pipe, PipeTransform } from '@angular/core';
import { LanguageService } from '../services/language.service';

@Pipe({
  name: 't',
  standalone: true,
  pure: false
})
export class TranslatePipe implements PipeTransform {
  constructor(private languageService: LanguageService) {}

  transform(key: string): string {
    // Read the language signal to create a dependency
    // This ensures the pipe re-evaluates when language changes
    this.languageService.language();
    return this.languageService.t(key);
  }
}
