import { useState, useEffect } from 'react';
import api from '../api/axiosConfig';

export default function DataSetupPanel() {
    const [activeTab, setActiveTab] = useState('carrera');
    const [message, setMessage] = useState(null);

    const [careers, setCareers] = useState([]);
    const [studyPlans, setStudyPlans] = useState([]);
    const [subjectsList, setSubjectsList] = useState([]);

    const [careerData, setCareerData] = useState({ code: '', name: '', description: '' });
    const [teacherData, setTeacherData] = useState({ run: '', fullName: '', email: '', professionalTitle: '', academicDegree: '' });
    const [studentData, setStudentData] = useState({ run: '', fullName: '', email: '', careerId: '' });
    const [planData, setPlanData] = useState({ code: '', name: '', careerId: '', setAsCurrent: true });

    const [selectedCareerForSubject, setSelectedCareerForSubject] = useState('');
    const [subjectData, setSubjectData] = useState({ code: '', name: '', semester: 1, hoursTheory: 2, hoursExercise: 2, hoursLab: 0, creditsSct: 5, studyPlanId: '' });

    const fetchCareers = () => {
        api.get('/careers').then(res => setCareers(res.data)).catch(console.error);
    };

    useEffect(() => { fetchCareers(); }, []);

    useEffect(() => {
        if (selectedCareerForSubject) {
            api.get(`/careers/${selectedCareerForSubject}/study-plans`)
                .then(res => setStudyPlans(res.data))
                .catch(console.error);
        } else {
            setStudyPlans([]);
        }
    }, [selectedCareerForSubject]);

    useEffect(() => {
        if (subjectData.studyPlanId) {
            api.get(`/study-plans/${subjectData.studyPlanId}/subjects`)
                .then(res => setSubjectsList(res.data))
                .catch(console.error);
        } else {
            setSubjectsList([]);
        }
    }, [subjectData.studyPlanId]);

    const showMsg = (type, text) => {
        setMessage({ type, text });
        setTimeout(() => setMessage(null), 5000);
    };

    const handleCareerSubmit = (e) => {
        e.preventDefault();
        api.post('/careers', careerData)
            .then(() => { showMsg('success', 'Carrera creada'); fetchCareers(); setCareerData({ code: '', name: '', description: '' }); })
            .catch(err => showMsg('error', err.response?.data?.message || 'Error al crear carrera'));
    };

    const handleDeleteCareer = (id) => {
        if (!window.confirm('¿Seguro que deseas borrar esta carrera?')) return;
        api.delete(`/careers/${id}`)
            .then(() => { showMsg('success', 'Carrera eliminada'); fetchCareers(); })
            .catch(err => showMsg('error', err.response?.data?.message || 'Error al eliminar carrera.'));
    };

    const handlePlanSubmit = (e) => {
        e.preventDefault();
        api.post(`/careers/${planData.careerId}/study-plans`, planData)
            .then(() => { showMsg('success', 'Plan creado'); setPlanData({ code: '', name: '', careerId: '', setAsCurrent: true }); })
            .catch(err => showMsg('error', err.response?.data?.message || 'Error al crear plan'));
    };

    const handleSubjectSubmit = (e) => {
        e.preventDefault();
        api.post(`/study-plans/${subjectData.studyPlanId}/subjects`, subjectData)
            .then((res) => {
                showMsg('success', 'Asignatura creada');
                setSubjectsList([...subjectsList, res.data]);
                setSubjectData({ ...subjectData, code: '', name: '' });
            })
            .catch(err => showMsg('error', err.response?.data?.message || 'Error al crear asignatura'));
    };

    const handleDeleteSubject = (id) => {
        if (!window.confirm('¿Seguro que deseas borrar esta asignatura?')) return;
        api.delete(`/study-plans/${subjectData.studyPlanId}/subjects/${id}`)
            .then(() => {
                showMsg('success', 'Asignatura eliminada');
                setSubjectsList(subjectsList.filter(s => s.id !== id));
            })
            .catch(err => showMsg('error', err.response?.data?.message || 'Error al eliminar asignatura.'));
    };

    // NUEVO: Funciones para manejar Prerrequisitos
    const handleAddPrerequisite = (subjectId, prerequisiteId) => {
        if (!prerequisiteId) return;
        api.post(`/study-plans/${subjectData.studyPlanId}/subjects/${subjectId}/prerequisites/${prerequisiteId}`)
            .then(res => {
                showMsg('success', 'Prerrequisito agregado con éxito');
                // Actualizamos solo la asignatura modificada en la lista
                setSubjectsList(subjectsList.map(s => s.id === subjectId ? res.data : s));
            })
            .catch(err => showMsg('error', err.response?.data?.message || 'Error al agregar prerrequisito'));
    };

    const handleRemovePrerequisite = (subjectId, prerequisiteId) => {
        if (!window.confirm('¿Remover este prerrequisito?')) return;
        api.delete(`/study-plans/${subjectData.studyPlanId}/subjects/${subjectId}/prerequisites/${prerequisiteId}`)
            .then(res => {
                showMsg('success', 'Prerrequisito removido');
                setSubjectsList(subjectsList.map(s => s.id === subjectId ? res.data : s));
            })
            .catch(err => showMsg('error', err.response?.data?.message || 'Error al remover prerrequisito'));
    };

    const handleTeacherSubmit = (e) => {
        e.preventDefault();
        api.post('/teachers', teacherData)
            .then(() => { showMsg('success', 'Docente registrado'); setTeacherData({ run: '', fullName: '', email: '', professionalTitle: '', academicDegree: '' }); })
            .catch(err => showMsg('error', err.response?.data?.message || 'Error al registrar docente'));
    };

    const handleStudentSubmit = (e) => {
        e.preventDefault();
        api.post('/students', studentData)
            .then(() => { showMsg('success', 'Estudiante registrado'); setStudentData({ run: '', fullName: '', email: '', careerId: '' }); })
            .catch(err => showMsg('error', err.response?.data?.message || 'Error al registrar estudiante'));
    };

    const tabs = [
        { id: 'carrera', label: '1. Carreras' },
        { id: 'plan', label: '2. Planes' },
        { id: 'asignatura', label: '3. Asignaturas' },
        { id: 'docente', label: '4. Docentes' },
        { id: 'estudiante', label: '5. Estudiantes' }
    ];

    return (
        <div className="bg-white p-6 rounded shadow max-w-4xl mx-auto mt-8 border-t-4 border-indigo-600">
            <h2 className="text-2xl font-bold mb-6 text-gray-800">Registros Base del Sistema</h2>
            {message && <div className={`p-4 rounded font-medium mb-6 ${message.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>{message.text}</div>}

            <div className="flex border-b mb-6 overflow-x-auto">
                {tabs.map(tab => (
                    <button key={tab.id} onClick={() => setActiveTab(tab.id)} className={`py-2 px-4 font-semibold ${activeTab === tab.id ? 'border-b-2 border-indigo-600 text-indigo-600' : 'text-gray-500 hover:text-gray-700'}`}>
                        {tab.label}
                    </button>
                ))}
            </div>

            {activeTab === 'carrera' && (
                <>
                    <form onSubmit={handleCareerSubmit} className="space-y-4">
                        <div className="grid grid-cols-2 gap-4">
                            <div><label className="block text-sm font-medium">Código</label><input type="text" value={careerData.code} onChange={e => setCareerData({ ...careerData, code: e.target.value })} className="w-full border rounded p-2" required /></div>
                            <div><label className="block text-sm font-medium">Nombre</label><input type="text" value={careerData.name} onChange={e => setCareerData({ ...careerData, name: e.target.value })} className="w-full border rounded p-2" required /></div>
                        </div>
                        <div><label className="block text-sm font-medium">Descripción</label><input type="text" value={careerData.description} onChange={e => setCareerData({ ...careerData, description: e.target.value })} className="w-full border rounded p-2" /></div>
                        <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded">Crear Carrera</button>
                    </form>

                    <div className="mt-8 border-t pt-6">
                        <h3 className="text-lg font-bold mb-4 text-gray-700">Carreras Existentes</h3>
                        {careers.length > 0 ? (
                            <ul className="space-y-2">
                                {careers.map(c => (
                                    <li key={c.id} className="flex justify-between items-center bg-gray-50 p-3 rounded border border-gray-200">
                                        <span className="font-medium text-gray-800">{c.code} - {c.name}</span>
                                        <button onClick={() => handleDeleteCareer(c.id)} className="bg-red-100 text-red-700 px-3 py-1 rounded text-sm font-bold hover:bg-red-200">Borrar</button>
                                    </li>
                                ))}
                            </ul>
                        ) : <p className="text-sm text-gray-500">No hay carreras registradas.</p>}
                    </div>
                </>
            )}

            {activeTab === 'plan' && (
                <form onSubmit={handlePlanSubmit} className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                        <div><label className="block text-sm font-medium">Carrera</label><select value={planData.careerId} onChange={e => setPlanData({ ...planData, careerId: e.target.value })} className="w-full border rounded p-2" required><option value="">Seleccione...</option>{careers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
                        <div><label className="block text-sm font-medium">Código Plan (Ej. 2027.1)</label><input type="text" value={planData.code} onChange={e => setPlanData({ ...planData, code: e.target.value })} className="w-full border rounded p-2" required /></div>
                        <div><label className="block text-sm font-medium">Nombre Plan</label><input type="text" value={planData.name} onChange={e => setPlanData({ ...planData, name: e.target.value })} className="w-full border rounded p-2" required /></div>
                    </div>
                    <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded">Crear Plan</button>
                </form>
            )}

            {activeTab === 'asignatura' && (
                <>
                    <form onSubmit={handleSubjectSubmit} className="space-y-4">
                        <div className="grid grid-cols-2 gap-4">
                            <div><label className="block text-sm font-medium">1. Carrera</label><select value={selectedCareerForSubject} onChange={e => setSelectedCareerForSubject(e.target.value)} className="w-full border rounded p-2" required><option value="">Seleccione...</option>{careers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
                            <div><label className="block text-sm font-medium">2. Plan de Estudios</label><select value={subjectData.studyPlanId} onChange={e => setSubjectData({ ...subjectData, studyPlanId: e.target.value })} className="w-full border rounded p-2" required disabled={!selectedCareerForSubject}><option value="">Seleccione...</option>{studyPlans.map(p => <option key={p.id} value={p.id}>{p.code}</option>)}</select></div>
                            <div><label className="block text-sm font-medium">Código (Ej. TAP101)</label><input type="text" value={subjectData.code} onChange={e => setSubjectData({ ...subjectData, code: e.target.value })} className="w-full border rounded p-2" required /></div>
                            <div><label className="block text-sm font-medium">Nombre</label><input type="text" value={subjectData.name} onChange={e => setSubjectData({ ...subjectData, name: e.target.value })} className="w-full border rounded p-2" required /></div>
                            <div><label className="block text-sm font-medium">Semestre (1-10)</label><input type="number" min="1" max="10" value={subjectData.semester} onChange={e => setSubjectData({ ...subjectData, semester: e.target.value })} className="w-full border rounded p-2" required /></div>
                            <div><label className="block text-sm font-medium">Horas Teoría</label><input type="number" value={subjectData.hoursTheory} onChange={e => setSubjectData({ ...subjectData, hoursTheory: e.target.value })} className="w-full border rounded p-2" required /></div>
                            <div><label className="block text-sm font-medium">Horas Ejercicio</label><input type="number" value={subjectData.hoursExercise} onChange={e => setSubjectData({ ...subjectData, hoursExercise: e.target.value })} className="w-full border rounded p-2" required /></div>
                            <div><label className="block text-sm font-medium">Horas Laboratorio</label><input type="number" value={subjectData.hoursLab} onChange={e => setSubjectData({ ...subjectData, hoursLab: e.target.value })} className="w-full border rounded p-2" required /></div>
                            <div><label className="block text-sm font-medium">Créditos SCT</label><input type="number" value={subjectData.creditsSct} onChange={e => setSubjectData({ ...subjectData, creditsSct: e.target.value })} className="w-full border rounded p-2" required /></div>
                        </div>
                        <button type="submit" disabled={!subjectData.studyPlanId} className="bg-indigo-600 text-white px-4 py-2 rounded disabled:bg-gray-400">Crear Asignatura</button>
                    </form>

                    {/* LISTA DE ASIGNATURAS CON GESTIÓN DE PRERREQUISITOS */}
                    {subjectData.studyPlanId && (
                        <div className="mt-8 border-t pt-6">
                            <h3 className="text-lg font-bold mb-4 text-gray-700">Asignaturas de este Plan</h3>
                            {subjectsList.length > 0 ? (
                                <ul className="space-y-4">
                                    {subjectsList.map(s => (
                                        <li key={s.id} className="flex flex-col bg-gray-50 p-4 rounded border border-gray-200">

                                            {/* Cabecera de la asignatura */}
                                            <div className="flex justify-between items-start mb-2">
                                                <div>
                                                    <span className="font-bold text-gray-800 text-lg">{s.code} - {s.name}</span>
                                                    <p className="text-sm text-gray-500">Semestre {s.semester} | {s.creditsSct} SCT | TEL: {s.hoursTheory}-{s.hoursExercise}-{s.hoursLab}</p>
                                                </div>
                                                <button onClick={() => handleDeleteSubject(s.id)} className="bg-red-100 text-red-700 px-3 py-1 rounded text-sm font-bold hover:bg-red-200">Borrar</button>
                                            </div>

                                            {/* Sección de Prerrequisitos */}
                                            <div className="mt-2 pt-3 border-t border-gray-200 text-sm">
                                                <div className="mb-2">
                                                    <span className="font-semibold text-gray-700">Prerrequisitos actuales: </span>
                                                    {s.prerequisites && s.prerequisites.length > 0 ? (
                                                        s.prerequisites.map(p => (
                                                            <span key={p.id} className="inline-flex items-center bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded mr-2 mt-1">
                                                                {p.code} - {p.name}
                                                                <button type="button" onClick={() => handleRemovePrerequisite(s.id, p.id)} className="ml-2 text-red-500 font-bold hover:text-red-700">X</button>
                                                            </span>
                                                        ))
                                                    ) : <span className="text-gray-500 text-xs mr-2">Ninguno</span>}
                                                </div>

                                                {/* Formulario en línea para agregar un nuevo prerrequisito */}
                                                <div className="flex gap-2 mt-3 items-center">
                                                    <select id={`prereq-select-${s.id}`} className="border rounded p-1.5 text-xs flex-1 bg-white">
                                                        <option value="">Añadir un prerrequisito a {s.code}...</option>
                                                        {/* Filtramos para no mostrarse a sí misma ni a las que ya son prerrequisito */}
                                                        {subjectsList
                                                            .filter(cand => cand.id !== s.id && !(s.prerequisites || []).some(pr => pr.id === cand.id))
                                                            .map(cand => (
                                                                <option key={cand.id} value={cand.id}>{cand.code} - {cand.name}</option>
                                                            ))
                                                        }
                                                    </select>
                                                    <button
                                                        type="button"
                                                        onClick={() => {
                                                            const selectEl = document.getElementById(`prereq-select-${s.id}`);
                                                            handleAddPrerequisite(s.id, selectEl.value);
                                                            selectEl.value = ''; // Limpiar tras seleccionar
                                                        }}
                                                        className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1.5 rounded text-xs font-bold transition"
                                                    >
                                                        Agregar
                                                    </button>
                                                </div>
                                            </div>

                                        </li>
                                    ))}
                                </ul>
                            ) : <p className="text-sm text-gray-500">No hay asignaturas en este plan.</p>}
                        </div>
                    )}
                </>
            )}

            {activeTab === 'docente' && (
                <form onSubmit={handleTeacherSubmit} className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                        <div><label className="block text-sm font-medium">RUN</label><input type="text" value={teacherData.run} onChange={e => setTeacherData({ ...teacherData, run: e.target.value })} className="w-full border rounded p-2" required /></div>
                        <div><label className="block text-sm font-medium">Nombre</label><input type="text" value={teacherData.fullName} onChange={e => setTeacherData({ ...teacherData, fullName: e.target.value })} className="w-full border rounded p-2" required /></div>
                        <div><label className="block text-sm font-medium">Email</label><input type="email" value={teacherData.email} onChange={e => setTeacherData({ ...teacherData, email: e.target.value })} className="w-full border rounded p-2" required /></div>
                        <div><label className="block text-sm font-medium">Título</label><input type="text" value={teacherData.professionalTitle} onChange={e => setTeacherData({ ...teacherData, professionalTitle: e.target.value })} className="w-full border rounded p-2" required /></div>
                        <div><label className="block text-sm font-medium">Grado</label><input type="text" value={teacherData.academicDegree} onChange={e => setTeacherData({ ...teacherData, academicDegree: e.target.value })} className="w-full border rounded p-2" required /></div>
                    </div>
                    <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded">Registrar Docente</button>
                </form>
            )}

            {activeTab === 'estudiante' && (
                <form onSubmit={handleStudentSubmit} className="space-y-4">
                    <p className="text-sm text-gray-500 mb-4">El RUN DEBE coincidir con el usuario en Keycloak.</p>
                    <div className="grid grid-cols-2 gap-4">
                        <div><label className="block text-sm font-medium">RUN</label><input type="text" value={studentData.run} onChange={e => setStudentData({ ...studentData, run: e.target.value })} className="w-full border rounded p-2" required /></div>
                        <div><label className="block text-sm font-medium">Nombre</label><input type="text" value={studentData.fullName} onChange={e => setStudentData({ ...studentData, fullName: e.target.value })} className="w-full border rounded p-2" required /></div>
                        <div><label className="block text-sm font-medium">Email</label><input type="email" value={studentData.email} onChange={e => setStudentData({ ...studentData, email: e.target.value })} className="w-full border rounded p-2" required /></div>
                        <div><label className="block text-sm font-medium">Carrera</label><select value={studentData.careerId} onChange={e => setStudentData({ ...studentData, careerId: e.target.value })} className="w-full border rounded p-2" required><option value="">Seleccione...</option>{careers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
                    </div>
                    <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded">Registrar Estudiante</button>
                </form>
            )}
        </div>
    );
}