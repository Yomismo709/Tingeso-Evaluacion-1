import { useEffect, useState } from 'react';
import api from '../api/axiosConfig';
import keycloak from '../keycloak';

export default function StudentCurriculum() {
    const [subjects, setSubjects] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const run = keycloak.tokenParsed?.preferred_username;

        // 1. Obtener al estudiante actual
        api.get(`/students/run/${run}`)
            .then(res => {
                const student = res.data;
                // 2. Obtener los planes de su carrera
                return api.get(`/careers/${student.careerId}/study-plans`);
            })
            .then(res => {
                const plans = res.data;
                // 3. Seleccionamos el plan vigente (o el primero como fallback)
                const activePlan = plans.find(p => p.status === 'VIGENTE') || plans[0];
                if (!activePlan) throw new Error("No hay plan de estudios configurado.");
                // 4. Obtener las asignaturas de ese plan
                return api.get(`/study-plans/${activePlan.id}/subjects`);
            })
            .then(res => {
                // Ordenamos las asignaturas por semestre
                const sortedSubjects = res.data.sort((a, b) => a.semester - b.semester);
                setSubjects(sortedSubjects);
                setLoading(false);
            })
            .catch(err => {
                console.error(err);
                setError('No se pudo cargar la malla curricular.');
                setLoading(false);
            });
    }, []);

    if (loading) return <div className="p-4">Cargando malla curricular...</div>;
    if (error) return <div className="p-4 text-red-500 font-bold">{error}</div>;

    return (
        <div className="bg-white p-6 rounded shadow max-w-4xl mx-auto mt-8 border-t-4 border-blue-600">
            <h2 className="text-2xl font-bold mb-6 text-gray-800">Mi Malla Curricular</h2>

            <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                    <thead>
                        <tr className="bg-gray-100 text-gray-700 border-b-2 border-gray-300">
                            <th className="p-3">Semestre</th>
                            <th className="p-3">Código</th>
                            <th className="p-3">Asignatura</th>
                            <th className="p-3">Créditos</th>
                            <th className="p-3">TEL</th>
                            <th className="p-3">Prerrequisitos</th>
                        </tr>
                    </thead>
                    <tbody>
                        {subjects.map((sub) => (
                            <tr key={sub.id} className="border-b hover:bg-gray-50">
                                <td className="p-3 font-bold text-blue-800 text-center">{sub.semester}</td>
                                <td className="p-3 font-medium text-gray-900">{sub.code}</td>
                                <td className="p-3">{sub.name}</td>
                                <td className="p-3 text-center">{sub.creditsSct}</td>
                                <td className="p-3 text-sm text-gray-600">
                                    {sub.hoursTheory}-{sub.hoursExercise}-{sub.hoursLab}
                                </td>
                                <td className="p-3 text-sm text-gray-500 font-medium">
                                    {sub.prerequisites && sub.prerequisites.length > 0
                                        ? sub.prerequisites.map(p => p.code).join(', ')
                                        : <span className="text-green-600">Ninguno</span>}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}