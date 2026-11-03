import { useEffect, useState } from 'react';
import api from '../api/axiosConfig';
import keycloak from '../keycloak';

export default function EnrollmentPanel() {
  const [periods, setPeriods] = useState([]);
  const [selectedPeriodId, setSelectedPeriodId] = useState('');
  
  // Obtenemos el estudiante dinámicamente desde el token
  const [currentStudent, setCurrentStudent] = useState(null);
  
  const [offer, setOffer] = useState([]);
  const [selectedSectionIds, setSelectedSectionIds] = useState([]);
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(true);

  // 1. Cargar el estudiante actual y los períodos
  useEffect(() => {
    const run = keycloak.tokenParsed?.preferred_username;
    
    // Ejecutar ambas peticiones en paralelo
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
      console.error("Error inicial:", err);
      setMessage({ type: 'error', text: 'Error al cargar los datos del estudiante o períodos.' });
      setLoading(false);
    });
  }, []);

  // 2. Cargar oferta académica usando el ID real del estudiante
  useEffect(() => {
    if (selectedPeriodId && currentStudent?.id) {
      api.get(`/academic-periods/${selectedPeriodId}/offer?studentId=${currentStudent.id}`)
        .then(res => {
          setOffer(res.data);
          setMessage(null);
        })
        .catch(err => {
          console.error(err);
          setOffer([]);
          setMessage({ type: 'error', text: err.response?.data?.message || 'Error al cargar la oferta académica.' });
        });
    } else {
      setOffer([]);
    }
  }, [selectedPeriodId, currentStudent]);

  const toggleSection = (sectionId) => {
    setSelectedSectionIds(prev => 
      prev.includes(sectionId) 
        ? prev.filter(id => id !== sectionId) 
        : [...prev, sectionId]
    );
  };

  const handleEnroll = (e) => {
    e.preventDefault();
    if (selectedSectionIds.length === 0) {
      setMessage({ type: 'error', text: 'Debes seleccionar al menos una asignatura.' });
      return;
    }

    const payload = {
      studentId: currentStudent.id,
      sectionIds: selectedSectionIds
    };

    api.post(`/academic-periods/${selectedPeriodId}/enrollments`, payload)
      .then(() => {
        setMessage({ type: 'success', text: '¡Asignaturas inscritas con éxito!' });
        setSelectedSectionIds([]);
        api.get(`/academic-periods/${selectedPeriodId}/offer?studentId=${currentStudent.id}`)
          .then(res => setOffer(res.data));
      })
      .catch((err) => setMessage({ type: 'error', text: err.response?.data?.message || 'Error al inscribir asignaturas.' }));
  };

  if (loading) return <div className="p-4">Cargando módulo de inscripción...</div>;

  return (
    <div className="bg-white p-6 rounded shadow max-w-4xl mx-auto mt-8 border-t-4 border-blue-600">
      <h2 className="text-2xl font-bold mb-6 text-gray-800">Inscripción de Asignaturas (Épica 6)</h2>
      
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

      {offer.length > 0 ? (
        <form onSubmit={handleEnroll}>
          <div className="space-y-4 mb-6">
            {offer.map((item) => (
              <div key={item.section.id} className={`border p-4 rounded flex items-start gap-4 transition ${selectedSectionIds.includes(item.section.id) ? 'border-blue-500 bg-blue-50' : 'border-gray-200'}`}>
                <div className="pt-1">
                  <input 
                    type="checkbox" 
                    className="w-5 h-5 cursor-pointer"
                    checked={selectedSectionIds.includes(item.section.id)}
                    onChange={() => toggleSection(item.section.id)}
                    disabled={item.availableSeats === 0}
                  />
                </div>
                <div className="flex-1">
                  <div className="flex justify-between items-start">
                    <h3 className="font-bold text-lg">{item.section.subjectCode} - {item.section.subjectName}</h3>
                    <span className={`text-sm font-bold px-2 py-1 rounded ${item.availableSeats > 0 ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                      Cupos: {item.availableSeats}
                    </span>
                  </div>
                  <p className="text-gray-600 text-sm mt-1">Profesor: {item.section.teacherName}</p>
                  
                  <div className="mt-3 flex gap-2 flex-wrap">
                    {item.section.scheduleBlocks.map((block, idx) => (
                      <span key={idx} className="bg-gray-200 text-gray-800 text-xs px-2 py-1 rounded border border-gray-300">
                        {block.dayCode} - Módulo {block.module}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>

          <button 
            type="submit" 
            disabled={selectedSectionIds.length === 0}
            className="w-full bg-blue-600 text-white font-bold py-3 rounded hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition shadow-lg"
          >
            Inscribir {selectedSectionIds.length} asignatura(s)
          </button>
        </form>
      ) : selectedPeriodId ? (
        <div className="text-center p-8 border-2 border-dashed border-gray-300 rounded text-gray-500">
          No hay asignaturas disponibles para tu plan de estudios en este período.
        </div>
      ) : null}
    </div>
  );
}