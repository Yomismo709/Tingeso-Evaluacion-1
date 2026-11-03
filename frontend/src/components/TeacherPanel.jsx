import { useEffect, useState } from 'react';
import api from '../api/axiosConfig';
import keycloak from '../keycloak';

export default function TeacherPanel() {
  const [periods, setPeriods] = useState([]);
  const [selectedPeriodId, setSelectedPeriodId] = useState('');
  
  const [currentTeacher, setCurrentTeacher] = useState(null);
  const [teacherSections, setTeacherSections] = useState([]);
  const [selectedSectionId, setSelectedSectionId] = useState('');
  
  const [enrollments, setEnrollments] = useState([]);
  const [grades, setGrades] = useState({}); // Guardará temporalmente lo que el profesor digite { enrollmentId: '5.5' }
  
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(true);

  // 1. Cargar datos del profesor conectado y períodos académicos
  useEffect(() => {
    const run = keycloak.tokenParsed?.preferred_username;
    
    Promise.all([
      api.get(`/teachers/run/${run}`),
      api.get('/academic-periods')
    ])
    .then(([teacherRes, periodsRes]) => {
      setCurrentTeacher(teacherRes.data);
      setPeriods(periodsRes.data);
      setLoading(false);
    })
    .catch(err => {
      console.error(err);
      setMessage({ type: 'error', text: 'Error al inicializar la sesión del profesor. ¿Existe este profesor en la base de datos?' });
      setLoading(false);
    });
  }, []);

  // 2. Cargar las secciones asignadas al profesor cuando selecciona un período
  useEffect(() => {
    if (selectedPeriodId && currentTeacher) {
      // CORRECCIÓN: Usamos la ruta exclusiva del profesor en lugar de la ruta global
      api.get(`/teachers/${currentTeacher.id}/sections`)
        .then(res => {
          // Filtramos localmente para mostrar solo las secciones que coinciden con el período seleccionado
          const mySections = res.data.filter(s => s.academicPeriodId === parseInt(selectedPeriodId));
          setTeacherSections(mySections);
          setSelectedSectionId('');
          setEnrollments([]);
        })
        .catch(err => {
          console.error("Error al cargar secciones del profesor:", err);
          setTeacherSections([]);
        });
    } else {
      setTeacherSections([]);
    }
  }, [selectedPeriodId, currentTeacher]);

  // 3. Cargar el listado de alumnos (enrollments) cuando selecciona una sección
  useEffect(() => {
    if (selectedPeriodId && selectedSectionId && currentTeacher) {
      api.get(`/academic-periods/${selectedPeriodId}/sections/${selectedSectionId}/enrollments?teacherId=${currentTeacher.id}`)
        .then(res => {
          setEnrollments(res.data);
          setMessage(null);
          // Inicializar el estado local de notas con las que ya existan en la BD
          const initialGrades = {};
          res.data.forEach(e => {
            if (e.finalGrade) initialGrades[e.id] = e.finalGrade;
          });
          setGrades(initialGrades);
        })
        .catch(err => {
          console.error(err);
          setEnrollments([]);
          setMessage({ type: 'error', text: 'Error al cargar la lista de estudiantes.' });
        });
    } else {
      setEnrollments([]);
    }
  }, [selectedPeriodId, selectedSectionId, currentTeacher]);

  const handleGradeChange = (enrollmentId, value) => {
    setGrades(prev => ({ ...prev, [enrollmentId]: value }));
  };

  const handleSaveGrade = (enrollmentId) => {
    const gradeValue = parseFloat(grades[enrollmentId]);
    if (isNaN(gradeValue) || gradeValue < 1.0 || gradeValue > 7.0) {
      setMessage({ type: 'error', text: 'La nota debe ser un número entre 1.0 y 7.0.' });
      return;
    }

    const payload = {
      teacherId: currentTeacher.id,
      grade: gradeValue
    };

    api.put(`/academic-periods/${selectedPeriodId}/sections/${selectedSectionId}/enrollments/${enrollmentId}/grade`, payload)
      .then(() => {
        setMessage({ type: 'success', text: 'Calificación guardada correctamente.' });
      })
      .catch(err => setMessage({ type: 'error', text: err.response?.data?.message || 'Error al guardar calificación.' }));
  };

  if (loading) return <div className="p-4">Cargando módulo docente...</div>;

  return (
    <div className="bg-white p-6 rounded shadow max-w-5xl mx-auto mt-8 border-t-4 border-indigo-600">
      <h2 className="text-2xl font-bold mb-2 text-gray-800">Panel Docente: Calificaciones</h2>
      {currentTeacher && <p className="text-gray-600 mb-6">Bienvenido/a, Profesor(a) {currentTeacher.fullName}</p>}
      
      {message && (
        <div className={`p-4 rounded font-medium mb-6 ${message.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
          {message.text}
        </div>
      )}

      <div className="grid grid-cols-2 gap-4 mb-8 bg-gray-50 p-4 rounded border">
        <div>
          <label className="block text-sm font-medium text-gray-700">1. Período Académico</label>
          <select value={selectedPeriodId} onChange={(e) => setSelectedPeriodId(e.target.value)} className="mt-1 w-full border rounded p-2">
            <option value="">Seleccione un período...</option>
            {periods.map(p => <option key={p.id} value={p.id}>{p.year} - Semestre {p.semester}</option>)}
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700">2. Mis Asignaturas</label>
          <select 
            value={selectedSectionId} 
            onChange={(e) => setSelectedSectionId(e.target.value)} 
            className="mt-1 w-full border rounded p-2"
            disabled={!selectedPeriodId || teacherSections.length === 0}
          >
            <option value="">Seleccione una sección...</option>
            {teacherSections.map(s => <option key={s.id} value={s.id}>{s.subjectCode} - {s.subjectName}</option>)}
          </select>
        </div>
      </div>

      {enrollments.length > 0 ? (
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-100 text-gray-700 border-b-2 border-gray-300">
                <th className="p-3">Estudiante</th>
                <th className="p-3">Estado</th>
                <th className="p-3">Fecha Inscripción</th>
                <th className="p-3 w-48">Calificación Final</th>
                <th className="p-3 text-center">Acción</th>
              </tr>
            </thead>
            <tbody>
              {enrollments.map((enrollment) => (
                <tr key={enrollment.id} className="border-b hover:bg-gray-50">
                  <td className="p-3">
                    <p className="font-bold text-gray-900">{enrollment.studentFullName}</p>
                    <p className="text-xs text-gray-500 font-mono">{enrollment.studentRun}</p>
                  </td>

                  <td className="p-3">
                    <span className={`px-2 py-1 rounded text-xs font-bold ${enrollment.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                      {enrollment.status}
                    </span>
                  </td>
                  <td className="p-3 text-sm text-gray-600">
                    {new Date(enrollment.enrolledAt).toLocaleDateString('es-CL')}
                  </td>
                  <td className="p-3">
                    <input 
                      type="number" 
                      min="1.0" max="7.0" step="0.1"
                      value={grades[enrollment.id] || ''}
                      onChange={(e) => handleGradeChange(enrollment.id, e.target.value)}
                      className="w-full border rounded p-1 text-center"
                      placeholder="Ej. 5.5"
                    />
                  </td>
                  <td className="p-3 text-center">
                    <button 
                      onClick={() => handleSaveGrade(enrollment.id)}
                      className="bg-indigo-600 text-white px-3 py-1 rounded text-sm hover:bg-indigo-700 transition"
                    >
                      Guardar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : selectedSectionId ? (
        <div className="text-center p-8 border-2 border-dashed border-gray-300 rounded text-gray-500">
          No hay estudiantes inscritos en esta sección.
        </div>
      ) : null}
    </div>
  );
}