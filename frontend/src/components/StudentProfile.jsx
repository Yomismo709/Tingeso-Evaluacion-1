import { useEffect, useState } from 'react';
import api from '../api/axiosConfig';
import keycloak from '../keycloak';

export default function StudentProfile() {
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const run = keycloak.tokenParsed?.preferred_username;

  useEffect(() => {
    // Llamada al backend pasando automáticamente el JWT gracias al interceptor
    api.get(`/students/run/${run}`)
      .then((response) => {
        setStudent(response.data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError('No se pudo cargar la información del estudiante.');
        setLoading(false);
      });
  }, [run]);

  if (loading) return <div className="p-4">Cargando tu información...</div>;
  if (error) return <div className="p-4 text-red-500">{error}</div>;

  return (
    <div className="bg-white p-6 rounded shadow border-t-4 border-blue-600">
      <h2 className="text-2xl font-bold mb-4">Mi Perfil Académico</h2>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <p className="text-sm text-gray-500 font-semibold">Nombre Completo</p>
          <p className="text-lg">{student.fullName}</p>
        </div>
        <div>
          <p className="text-sm text-gray-500 font-semibold">RUN</p>
          <p className="text-lg">{student.run}</p>
        </div>
        <div>
          <p className="text-sm text-gray-500 font-semibold">Correo Electrónico</p>
          <p className="text-lg">{student.email}</p>
        </div>
        <div>
          <p className="text-sm text-gray-500 font-semibold">Estado Académico</p>
          <span className="bg-green-100 text-green-800 text-sm font-medium px-2.5 py-0.5 rounded">
            {student.status}
          </span>
        </div>
      </div>
    </div>
  );
}