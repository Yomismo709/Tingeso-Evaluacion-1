import { useEffect, useState } from 'react';
import api from '../api/axiosConfig';

export default function AdminPanel() {
  const [periods, setPeriods] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [message, setMessage] = useState(null);
  const [closureResult, setClosureResult] = useState(null); // Nuevo estado para los resultados del cierre

  const [year, setYear] = useState(new Date().getFullYear());
  const [semester, setSemester] = useState(1);

  const [selectedPeriodId, setSelectedPeriodId] = useState('');
  const [careers, setCareers] = useState([]);
  const [selectedCareer, setSelectedCareer] = useState('');
  const [studyPlans, setStudyPlans] = useState([]);
  const [selectedStudyPlan, setSelectedStudyPlan] = useState('');
  const [subjects, setSubjects] = useState([]);
  const [selectedSubject, setSelectedSubject] = useState('');
  const [selectedTeacher, setSelectedTeacher] = useState('');
  const [capacity, setCapacity] = useState(30);
  
  const [scheduleBlocks, setScheduleBlocks] = useState([{ dayCode: 'L', module: 1 }]);

  // Nuevo estado para el cierre de semestre
  const [selectedPeriodToClose, setSelectedPeriodToClose] = useState('');
  const [isClosing, setIsClosing] = useState(false);

  const fetchInitialData = () => {
    api.get('/academic-periods').then(res => setPeriods(res.data)).catch(console.error);
    api.get('/teachers').then(res => setTeachers(res.data)).catch(console.error);
    api.get('/careers').then(res => setCareers(res.data)).catch(console.error);
  };

  useEffect(() => {
    fetchInitialData();
  }, []);

  useEffect(() => {
    setSelectedStudyPlan('');
    setSubjects([]);
    if (selectedCareer) {
      api.get(`/careers/${selectedCareer}/study-plans`)
        .then(res => setStudyPlans(res.data))
        .catch(console.error);
    }
  }, [selectedCareer]);

  useEffect(() => {
    setSelectedSubject('');
    if (selectedStudyPlan) {
      api.get(`/study-plans/${selectedStudyPlan}/subjects`)
        .then(res => setSubjects(res.data))
        .catch(console.error);
    }
  }, [selectedStudyPlan]);

  useEffect(() => {
    setScheduleBlocks([{ dayCode: 'L', module: 1 }]);
  }, [selectedSubject]);

  const selectedSubjectData = subjects.find(s => s.id === parseInt(selectedSubject));
  const requiredBlocks = selectedSubjectData ? (selectedSubjectData.totalWeeklyHours / 2) : 0;
  const isBlocksValid = scheduleBlocks.length === requiredBlocks;

  const handleAddBlock = () => {
    if (scheduleBlocks.length < requiredBlocks) {
      setScheduleBlocks([...scheduleBlocks, { dayCode: 'L', module: 1 }]);
    }
  };

  const handleRemoveBlock = (index) => {
    setScheduleBlocks(scheduleBlocks.filter((_, i) => i !== index));
  };

  const handleBlockChange = (index, field, value) => {
    const newBlocks = [...scheduleBlocks];
    newBlocks[index][field] = value;
    setScheduleBlocks(newBlocks);
  };

  const handleCreatePeriod = (e) => {
    e.preventDefault();
    api.post('/academic-periods', { year: parseInt(year), semester: parseInt(semester) })
      .then(() => {
        setMessage({ type: 'success', text: 'Período abierto con éxito.' });
        fetchInitialData();
      })
      .catch((err) => setMessage({ type: 'error', text: err.response?.data?.message || 'Error al crear período.' }));
  };

  const handleCreateSection = (e) => {
    e.preventDefault();
    
    const payload = {
      subjectId: parseInt(selectedSubject),
      teacherId: parseInt(selectedTeacher),
      capacity: parseInt(capacity),
      scheduleBlocks: scheduleBlocks.map(block => ({
        dayCode: block.dayCode,
        module: parseInt(block.module)
      }))
    };

    api.post(`/academic-periods/${selectedPeriodId}/sections`, payload)
      .then(() => {
        setMessage({ type: 'success', text: 'Sección programada con éxito.' });
        setSelectedSubject('');
        setSelectedTeacher('');
        setScheduleBlocks([{ dayCode: 'L', module: 1 }]);
      })
      .catch((err) => setMessage({ type: 'error', text: err.response?.data?.message || 'Error al programar sección.' }));
  };

  // NUEVO: Algoritmo para invocar el cierre del período
  const handleClosePeriod = (e) => {
    e.preventDefault();
    if (!window.confirm('ADVERTENCIA: Esta acción es irreversible y consolidará el historial académico de todos los estudiantes. ¿Deseas continuar?')) return;
    
    setIsClosing(true);
    setClosureResult(null);
    setMessage(null);

    api.post(`/academic-periods/${selectedPeriodToClose}/close`)
      .then((res) => {
        setClosureResult(res.data);
        setMessage({ type: 'success', text: `El período ${res.data.academicPeriodCode} ha sido cerrado correctamente.` });
        fetchInitialData(); // Refrescar la lista de períodos (ahora estará cerrado)
        setSelectedPeriodToClose('');
      })
      .catch((err) => {
        const errorDetail = err.response?.data?.message || 'Error desconocido al intentar cerrar el período.';
        setMessage({ type: 'error', text: errorDetail });
      })
      .finally(() => setIsClosing(false));
  };

  return (
    <div className="space-y-6">
      {message && (
        <div className={`p-4 rounded font-medium shadow-sm ${message.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
          {message.text}
        </div>
      )}

      {/* Tarjeta de resultados de cierre (Si el cierre fue exitoso) */}
      {closureResult && (
        <div className="bg-blue-50 border-l-4 border-blue-600 p-6 rounded shadow-sm">
          <h3 className="text-xl font-bold text-blue-900 mb-4">Resumen del Cierre de Semestre</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-center">
            <div className="bg-white p-3 rounded shadow-sm border border-blue-100">
              <p className="text-sm text-gray-500 font-semibold">Notas Procesadas</p>
              <p className="text-2xl font-bold text-blue-700">{closureResult.gradedEnrollments}</p>
            </div>
            <div className="bg-white p-3 rounded shadow-sm border border-blue-100">
              <p className="text-sm text-gray-500 font-semibold">Inscripciones Aprobadas</p>
              <p className="text-2xl font-bold text-green-600">{closureResult.passedEnrollments}</p>
            </div>
            <div className="bg-white p-3 rounded shadow-sm border border-blue-100">
              <p className="text-sm text-gray-500 font-semibold">Alumnos Egresados</p>
              <p className="text-2xl font-bold text-indigo-600">{closureResult.graduatedStudents}</p>
            </div>
            <div className="bg-white p-3 rounded shadow-sm border border-blue-100">
              <p className="text-sm text-gray-500 font-semibold">Alumnos Eliminados</p>
              <p className="text-2xl font-bold text-red-600">{closureResult.expelledStudents}</p>
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* BLOQUE 1: ABRIR PERÍODO */}
        <div className="bg-white p-6 rounded shadow border-t-4 border-blue-800">
          <h2 className="text-xl font-bold mb-4">1. Abrir Período Académico</h2>
          <form onSubmit={handleCreatePeriod} className="space-y-4">
            <div className="flex gap-4">
              <div className="flex-1">
                <label className="block text-sm font-medium">Año</label>
                <input type="number" value={year} onChange={(e) => setYear(e.target.value)} className="mt-1 w-full border rounded p-2" required />
              </div>
              <div className="flex-1">
                <label className="block text-sm font-medium">Semestre</label>
                <select value={semester} onChange={(e) => setSemester(e.target.value)} className="mt-1 w-full border rounded p-2">
                  <option value={1}>1</option>
                  <option value={2}>2</option>
                </select>
              </div>
            </div>
            <button type="submit" className="w-full bg-blue-600 text-white p-2 rounded hover:bg-blue-700 font-semibold transition">Abrir Período</button>
          </form>
        </div>

        {/* NUEVO BLOQUE: CERRAR PERÍODO */}
        <div className="bg-white p-6 rounded shadow border-t-4 border-red-800">
          <h2 className="text-xl font-bold mb-4 text-red-800">3. Cerrar Período Académico</h2>
          <p className="text-sm text-gray-600 mb-4">
            El cierre consolida las notas en el historial, actualiza a los estudiantes eliminados/egresados y bloquea modificaciones futuras. Requiere que el 100% de los estudiantes tengan su calificación registrada.
          </p>
          <form onSubmit={handleClosePeriod} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">Período Abierto a Cerrar</label>
              <select 
                value={selectedPeriodToClose} 
                onChange={(e) => setSelectedPeriodToClose(e.target.value)} 
                className="mt-1 w-full border border-gray-300 rounded p-2" 
                required
              >
                <option value="">Seleccione un período para cerrar...</option>
                {/* Solo se muestran períodos que están en estado OPEN */}
                {periods.filter(p => p.status === 'OPEN').map(p => (
                  <option key={p.id} value={p.id}>{p.code}</option>
                ))}
              </select>
            </div>
            <button 
              type="submit" 
              disabled={!selectedPeriodToClose || isClosing}
              className="w-full bg-red-600 text-white p-2 rounded hover:bg-red-700 font-semibold disabled:bg-gray-400 disabled:cursor-not-allowed transition flex justify-center items-center gap-2"
            >
              {isClosing ? 'Procesando Cierre...' : 'Ejecutar Cierre de Semestre'}
            </button>
          </form>
        </div>

        {/* BLOQUE 2: PROGRAMAR SECCIÓN (Ocupará toda la fila de abajo en pantallas grandes) */}
        <div className="bg-white p-6 rounded shadow border-t-4 border-green-600 lg:col-span-2">
          <h2 className="text-xl font-bold mb-4">2. Programar Sección (Oferta Académica)</h2>
          <form onSubmit={handleCreateSection} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700">Período Académico (Abierto)</label>
                <select value={selectedPeriodId} onChange={(e) => setSelectedPeriodId(e.target.value)} className="mt-1 w-full border rounded p-2" required>
                  <option value="">0. Seleccione período...</option>
                  {periods.filter(p => p.status === 'OPEN').map(p => <option key={p.id} value={p.id}>{p.code}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Carrera</label>
                <select value={selectedCareer} onChange={(e) => setSelectedCareer(e.target.value)} className="mt-1 w-full border rounded p-2" required>
                  <option value="">1. Seleccione carrera...</option>
                  {careers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700">Plan de Estudios</label>
                <select value={selectedStudyPlan} onChange={(e) => setSelectedStudyPlan(e.target.value)} className="mt-1 w-full border rounded p-2" required disabled={!selectedCareer}>
                  <option value="">2. Seleccione plan...</option>
                  {studyPlans.map(sp => <option key={sp.id} value={sp.id}>{sp.code} - {sp.name}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Asignatura</label>
                <select value={selectedSubject} onChange={(e) => setSelectedSubject(e.target.value)} className="mt-1 w-full border rounded p-2" required disabled={!selectedStudyPlan}>
                  <option value="">3. Seleccione asignatura...</option>
                  {subjects.map(sub => <option key={sub.id} value={sub.id}>{sub.code} - {sub.name}</option>)}
                </select>
              </div>
            </div>

            <hr className="my-4 border-gray-200" />

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700">Profesor Asignado</label>
                <select value={selectedTeacher} onChange={(e) => setSelectedTeacher(e.target.value)} className="mt-1 w-full border rounded p-2" required>
                  <option value="">Seleccione un profesor...</option>
                  {teachers.map(t => <option key={t.id} value={t.id}>{t.fullName}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Cupo Máximo</label>
                <input type="number" min="1" max="60" value={capacity} onChange={(e) => setCapacity(e.target.value)} className="mt-1 w-full border rounded p-2" required />
              </div>
            </div>

            {/* SECCIÓN DINÁMICA DE HORARIOS */}
            {selectedSubject && (
              <div className="bg-gray-50 p-4 rounded border">
                <div className="flex justify-between items-center mb-2">
                  <h3 className="font-semibold text-gray-800">Bloques de Horario</h3>
                  <span className={`text-sm font-bold ${isBlocksValid ? 'text-green-600' : 'text-red-600'}`}>
                    {scheduleBlocks.length} de {requiredBlocks} requeridos por la malla
                  </span>
                </div>
                
                {scheduleBlocks.map((block, index) => (
                  <div key={index} className="flex gap-2 mb-2 items-end">
                    <div className="flex-1">
                      <select value={block.dayCode} onChange={(e) => handleBlockChange(index, 'dayCode', e.target.value)} className="w-full border rounded p-2 text-sm">
                        <option value="L">Lunes</option>
                        <option value="M">Martes</option>
                        <option value="W">Miércoles</option>
                        <option value="J">Jueves</option>
                        <option value="V">Viernes</option>
                        <option value="S">Sábado</option>
                      </select>
                    </div>
                    <div className="flex-1">
                      <select value={block.module} onChange={(e) => handleBlockChange(index, 'module', e.target.value)} className="w-full border rounded p-2 text-sm">
                        {[1, 2, 3, 4, 5, 6].map(num => (
                          <option key={num} value={num}>Módulo {num}</option>
                        ))}
                      </select>
                    </div>
                    {scheduleBlocks.length > 1 && (
                      <button type="button" onClick={() => handleRemoveBlock(index)} className="bg-red-100 text-red-600 px-3 py-2 rounded hover:bg-red-200">
                        X
                      </button>
                    )}
                  </div>
                ))}
                
                {!isBlocksValid && (
                  <button type="button" onClick={handleAddBlock} className="text-blue-600 text-sm font-medium hover:underline mt-2">
                    + Añadir otro bloque
                  </button>
                )}
              </div>
            )}
            
            <button 
              type="submit" 
              disabled={!isBlocksValid || !selectedSubject}
              className={`w-full p-3 rounded mt-4 font-semibold transition shadow-sm ${isBlocksValid && selectedSubject ? 'bg-green-600 text-white hover:bg-green-700' : 'bg-gray-300 text-gray-500 cursor-not-allowed'}`}
            >
              Programar Sección
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}