import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it } from 'vitest';
import { ProductTechnicalForm } from './product-technical-form';
import { TechnicalSchema } from './product-technical.models';

const cpu:TechnicalSchema={tipo:'cpu',nombre:'Procesador',campos:[
  {key:'socket',label:'Socket',type:'text',unit:null,integer:false,positive:false},
  {key:'cores',label:'Núcleos',type:'number',unit:null,integer:true,positive:true},
  {key:'tdp_w',label:'TDP declarado',type:'number',unit:'W',integer:false,positive:false},
  {key:'igpu',label:'Gráficos integrados',type:'boolean',unit:null,integer:false,positive:false},
]};
describe('Category-specific technical form',()=>{
  afterEach(()=>TestBed.resetTestingModule());
  function setup(){TestBed.configureTestingModule({imports:[ProductTechnicalForm]});const fixture=TestBed.createComponent(ProductTechnicalForm);fixture.componentRef.setInput('schema',cpu);fixture.detectChanges();return fixture;}
  it('renders collapsible schema fields and units',()=>{
    const fixture=setup();expect(fixture.nativeElement.querySelector('summary').textContent).toContain('Procesador');expect(fixture.nativeElement.textContent).toContain('TDP declarado (W)');expect(fixture.nativeElement.querySelector('details').open).toBe(false);
  });
  it('starts unknown fields blank instead of guessing zero or false',()=>{
    const fixture=setup();expect(fixture.componentInstance.values).toEqual({});expect(fixture.componentInstance.canSave()).toBe(true);expect(fixture.nativeElement.querySelector('select').value).toBe('');
  });
  it('preserves false and zero when explicitly supplied',()=>{
    const fixture=setup();fixture.componentRef.setInput('value',{tipo:'cpu',fuenteUrl:null,valores:{igpu:false,tdp_w:0}});fixture.detectChanges();expect(fixture.componentInstance.values['igpu']).toBe(false);expect(fixture.componentInstance.values['tdp_w']).toBe(0);expect(fixture.nativeElement.querySelector('select').value).toBe('false');
  });
  it('loads a saved specification without an initial destructive emission',()=>{
    const fixture=setup();let emitted=false;fixture.componentInstance.valueChange.subscribe(()=>emitted=true);fixture.componentRef.setInput('value',{tipo:'cpu',fuenteUrl:'https://example.com/spec',valores:{socket:'AM4',cores:6}});fixture.detectChanges();expect(fixture.componentInstance.source).toBe('https://example.com/spec');expect(fixture.componentInstance.values['cores']).toBe(6);expect(emitted).toBe(false);
  });
  it('explains recovered legacy data and warnings without marking them as a new edit',()=>{
    const fixture=setup();let emitted=false;fixture.componentInstance.valueChange.subscribe(()=>emitted=true);
    fixture.componentRef.setInput('value',{tipo:'cpu',origen:'ANTERIOR',avisos:['Revisar potencia base anterior.'],valores:{cores:6,igpu:false}});fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Ficha anterior recuperada');expect(fixture.nativeElement.textContent).toContain('Revisar potencia base anterior.');expect(emitted).toBe(false);
  });
  it('emits typed values and null on explicit clearing',()=>{
    const fixture=setup();let last:any;fixture.componentInstance.valueChange.subscribe(value=>last=value);const input=fixture.nativeElement.querySelector('input[type="number"]') as HTMLInputElement;input.value='6';input.dispatchEvent(new Event('input'));expect(last.valores.cores).toBe(6);input.value='';input.dispatchEvent(new Event('input'));expect(last.valores.cores).toBeNull();
  });
  it('blocks negative and fractional counts and non-finite numbers',()=>{
    const component=setup().componentInstance;for(const value of [-1,0,1.5,NaN]){component.values={cores:value};expect(component.canSave()).toBe(false);}component.values={cores:6,tdp_w:65};expect(component.canSave()).toBe(true);
  });
  it('validates HTTPS sources without credentials',()=>{
    const component=setup().componentInstance;for(const source of ['http://example.com','javascript:alert(1)','https://user:pass@example.com']){component.source=source;expect(component.canSave()).toBe(false);}component.source='https://example.com/spec';expect(component.canSave()).toBe(true);
  });
  it('does not carry CPU values into a PSU schema',()=>{
    const fixture=setup();fixture.componentRef.setInput('value',{tipo:'cpu',valores:{cores:6}});fixture.detectChanges();fixture.componentRef.setInput('schema',{tipo:'psu',nombre:'Fuente',campos:[]});fixture.detectChanges();expect(fixture.componentInstance.values).toEqual({});
  });
});
