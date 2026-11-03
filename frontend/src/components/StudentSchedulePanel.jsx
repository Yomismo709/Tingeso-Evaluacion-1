import { useEffect, useState } from 'react';
import api from '../api/axiosConfig';
import keycloak from '../keycloak';

export default function StudentSchedulePanel() {
  const [periods, setPeriods] = useState([]);
  const [selectedPeriodId, setSelectedPeriodId] = useState('');
  const [currentStudent, setCurrentStudent] = useState(null);
  
  const [enrollments, setEnrollments] = useState([]);
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const run = keycloak.tokenParsed?.preferred_username;
    
    Promise.all([
      api.get(`/students/run/${run}`),
      api.get('/academic-periods')
    ])
    .then(([studentRes, periodsRes]) => {
      setCurrentStudent(studentRes.data);
      setPeriods(periodsRes.data);
      setLoading(false);
    })
    .catch(err => {
      console.error(err);
      setMessage({ type: 'error', text: 'Error al inicializar los datos.' });
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    if (selectedPeriodId && currentStudent?.id) {
      api.get(`/academic-periods/${selectedPeriodId}/students/${currentStudent.id}/enrollments`)
        .then(res => {
          setEnrollments(res.data);
          setMessage(null);
        })
        .catch(err => {
          console.error(err);
          setEnrollments([]);
          setMessage({ type: 'error', text: 'Error al cargar las asignaturas inscritas.' });
        });
    } else {
      setEnrollments([]);
    }
  }, [selectedPeriodId, currentStudent]);

  const handleCancelEnrollment = (enrollmentId) => {
    if (!window.confirm('¿Estás seguro de que deseas anular la inscripción de esta asignatura?')) return;

    api.delete(`/academic-periods/${selectedPeriodId}/students/${currentStudent.id}/enrollments/${enrollmentId}`)
      .then(() => {
        setMessage({ type: 'success', text: 'Asignatura anulada con éxito.' });
        setEnrollments(enrollments.filter(e => e.id !== enrollmentId));
      })
      .catch(err => setMessage({ type: 'error', text: err.response?.data?.message || 'Error al anular la asignatura.' }));
  };

  if (loading) return <div className="p-4">Cargando horario...</div>;

  return (
    <div className="bg-white p-6 rounded shadow max-w-4xl mx-auto mt-8 border-t-4 border-green-600">
      <h2 className="text-2xl font-bold mb-6 text-gray-800">Mi Carga Académica</h2>
      
      {message && (
        <div className={`p-4 rounded font-medium mb-6 ${message.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
          {message.text}
        </div>
      )}

      <div className="mb-8 bg-gray-50 p-4 rounded border">
        <label className="block text-sm font-medium text-gray-700">Período Académico</label>
        <select value={selectedPeriodId} onChange={(e) => setSelectedPeriodId(e.target.value)} className="mt-1 w-full border rounded p-2">
          <option value="">Seleccione un período...</option>
          {periods.map(p => <option key={p.id} value={p.id}>{p.year} - Semestre {p.semester}</option>)}
        </select>
      </div>

      {enrollments.length > 0 ? (
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-100 text-gray-700 border-b-2 border-gray-300">
                <th className="p-3">Código</th>
                <th className="p-3">Asignatura</th>
                <th className="p-3">Estado</th>
                <th className="p-3">Fecha Inscripción</th>
                <th className="p-3 text-center">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {enrollments.map((enrollment) => (
                <tr key={enrollment.id} className="border-b hover:bg-gray-50">
                  <td className="p-3 font-medium text-gray-900">{enrollment.subjectCode}</td>
                  <td className="p-3">{enrollment.subjectName}</td>
                  <td className="p-3">
                    <span className={`px-2 py-1 rounded text-xs font-bold ${enrollment.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                      {enrollment.status}
                    </span>
                  </td>
                  <td className="p-3 text-sm text-gray-600">
                    {new Date(enrollment.enrolledAt).toLocaleDateString('es-CL')}
                  </td>
                  <td className="p-3 text-center">
                    {enrollment.status === 'ACTIVE' && (
                      <button 
                        onClick={() => handleCancelEnrollment(enrollment.id)}
                        className="text-red-600 hover:text-red-800 text-sm font-medium underline"
                      >
                        Anular
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : selectedPeriodId ? (
        <div className="text-center p-8 border-2 border-dashed border-gray-300 rounded text-gray-500">
          No tienes asignaturas inscritas en este período.
        </div>
      ) : null}
    </div>
  );
}