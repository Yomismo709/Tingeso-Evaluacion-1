import { useEffect, useState } from 'react';
import api from '../api/axiosConfig';

export default function MatriculationPanel() {
  const [periods, setPeriods] = useState([]);
  const [selectedPeriodId, setSelectedPeriodId] = useState('');
  
  // Estado para la búsqueda
  const [searchRun, setSearchRun] = useState('');
  const [foundStudent, setFoundStudent] = useState(null);
  const [isSearching, setIsSearching] = useState(false);
  
  const [message, setMessage] = useState(null);

  useEffect(() => {
    api.get('/academic-periods')
      .then(res => setPeriods(res.data))
      .catch(console.error);
  }, []);

  // Paso 1: Buscar estudiante por RUN
  const handleSearchStudent = (e) => {
    e.preventDefault();
    setIsSearching(true);
    setMessage(null);
    setFoundStudent(null);

    api.get(`/students/run/${searchRun}`)
      .then(res => {
        setFoundStudent(res.data);
      })
      .catch(err => {
        setMessage({ type: 'error', text: err.response?.data?.message || 'Estudiante no encontrado con ese RUN.' });
      })
      .finally(() => {
        setIsSearching(false);
      });
  };

  // Paso 2: Ejecutar la matrícula usando el ID oculto del estudiante encontrado
  const handleMatriculate = () => {
    if (!selectedPeriodId || !foundStudent) return;

    const payload = {
      studentId: foundStudent.id,
      matriculated: true
    };

    api.put(`/academic-periods/${selectedPeriodId}/matriculations`, payload)
      .then(() => {
        setMessage({ type: 'success', text: `¡Matrícula administrativa confirmada para ${foundStudent.fullName}!` });
        // Limpiar búsqueda tras éxito
        setFoundStudent(null);
        setSearchRun('');
      })
      .catch((err) => setMessage({ type: 'error', text: err.response?.data?.message || 'Error al procesar la matrícula.' }));
  };

  return (
    <div className="bg-white p-6 rounded shadow border-t-4 border-yellow-500 max-w-xl mx-auto mt-8">
      <h2 className="text-xl font-bold mb-4">Administración: Matricular Estudiante</h2>
      <p className="text-gray-600 mb-6 text-sm">
        Busca al estudiante por su RUN para otorgarle la matrícula administrativa en el período académico vigente.
      </p>

      {message && (
        <div className={`p-4 rounded font-medium mb-6 ${message.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
          {message.text}
        </div>
      )}

      <div className="space-y-6">
        {/* Selector de Período */}
        <div>
          <label className="block text-sm font-medium text-gray-700">1. Período Académico</label>
          <select value={selectedPeriodId} onChange={(e) => setSelectedPeriodId(e.target.value)} className="mt-1 w-full border rounded p-2" required>
            <option value="">Seleccione el período...</option>
            {periods.map(p => <option key={p.id} value={p.id}>{p.year} - Semestre {p.semester}</option>)}
          </select>
        </div>

        {/* Buscador de Estudiante */}
        <form onSubmit={handleSearchStudent}>
          <label className="block text-sm font-medium text-gray-700">2. Buscar por RUN (Ej. 12345678-9)</label>
          <div className="flex gap-2 mt-1">
            <input 
              type="text" 
              value={searchRun} 
              onChange={(e) => setSearchRun(e.target.value)} 
              placeholder="12345678-9"
              className="flex-1 border rounded p-2" 
              required 
            />
            <button 
              type="submit" 
              disabled={isSearching || !searchRun}
              className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:bg-gray-400"
            >
              {isSearching ? 'Buscando...' : 'Buscar'}
            </button>
          </div>
        </form>

        {/* Tarjeta de Confirmación */}
        {foundStudent && (
          <div className="border border-green-200 bg-green-50 p-4 rounded mt-4">
            <h3 className="font-bold text-green-800 mb-2">Estudiante Encontrado</h3>
            <p className="text-sm"><strong>Nombre:</strong> {foundStudent.fullName}</p>
            <p className="text-sm"><strong>RUN:</strong> {foundStudent.run}</p>
            <p className="text-sm"><strong>Carrera:</strong> {foundStudent.careerName}</p>
            <p className="text-sm mb-4"><strong>Estado:</strong> {foundStudent.status}</p>
            
            <button 
              onClick={handleMatriculate}
              disabled={!selectedPeriodId} 
              className="w-full bg-yellow-500 text-white font-bold p-2 rounded hover:bg-yellow-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition"
            >
              Confirmar Matrícula Administrativa
            </button>
          </div>
        )}
      </div>
    </div>
  );
}