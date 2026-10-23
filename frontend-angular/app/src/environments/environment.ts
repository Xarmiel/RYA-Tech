export const environment = {
  production: false,
  // En desarrollo, apiUrl vacía para enrutar /api mediante proxy.conf.json a http://localhost:8080
  apiUrl: '',
  supabaseUrl: 'https://rasudbiaxwyoojxxdutz.supabase.co',
  // Clave pública anon para clientes web (nunca incluir service_role aquí)
  supabaseAnonKey: 'sb_publishable_Lp3yRQwKYfpaajrT_CMmew_oCBPdmFT',
};
