import { Component, OnChanges, input, output } from '@angular/core';
import { ProductTechnicalFicha, TechnicalSchema, TechnicalSpecField } from './product-technical.models';
import { technicalIssues } from './product-technical-validation';

@Component({selector:'app-product-technical-form',templateUrl:'./product-technical-form.html',styleUrl:'./product-technical-form.scss'})
export class ProductTechnicalForm implements OnChanges {
  readonly schema=input<TechnicalSchema|null>(null);
  readonly value=input<ProductTechnicalFicha|null>(null);
  readonly disabled=input(false);
  readonly valueChange=output<ProductTechnicalFicha>();
  values:ProductTechnicalFicha['valores']={};source='';
  ngOnChanges(){const value=this.value();this.values=value?.tipo===this.schema()?.tipo?{...value?.valores}:{};this.source=value?.tipo===this.schema()?.tipo?value?.fuenteUrl??'':'';}
  setField(field:TechnicalSpecField,event:Event){
    const input=event.target as HTMLInputElement;
    const raw=input.value;
    const value=field.type==='boolean'?(raw===''?null:raw==='true'):field.type==='number'?(input.validity?.badInput?NaN:raw.trim()===''?null:Number(raw)):raw.trim()||null;
    this.values={...this.values,[field.key]:value};this.emit();
  }
  setSource(event:Event){this.source=(event.target as HTMLInputElement).value;this.emit();}
  private emit(){const schema=this.schema();if(schema)this.valueChange.emit({tipo:schema.tipo,fuenteUrl:this.source.trim()||null,valores:{...this.values}});}
  issues():string[]{
    const schema=this.schema();return schema?technicalIssues(schema,{tipo:schema.tipo,fuenteUrl:this.source,valores:this.values}):[];
  }
  canSave(){return this.issues().length===0;}
}
