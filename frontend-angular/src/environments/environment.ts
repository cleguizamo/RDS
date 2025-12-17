export const environment = {
  production: false,
  // Para desarrollo local usa localhost, para red local usa la IP del servidor
  // Si estás en otra máquina, cambia 'localhost' por la IP del servidor
  // IP actual detectada: 172.20.10.5 (puede cambiar si te conectas a otra red)
  apiUrl: 'http://localhost:8080/api'  // Usar localhost si frontend y backend están en la misma máquina
  
  // Si el frontend está en otra máquina, usar la IP del servidor:
  // apiUrl: 'http://172.20.10.5:8080/api'
};

