import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import keycloak from './keycloak';
import StudentProfile from './components/StudentProfile';
import AdminPanel from './components/AdminPanel';
import MatriculationPanel from './components/MatriculationPanel';
import EnrollmentPanel from './components/EnrollmentPanel';
import StudentSchedulePanel from './components/StudentSchedulePanel';
import TeacherPanel from './components/TeacherPanel';

function App() {
  const handleLogout = () => {
    keycloak.logout();
  };

  const hasRole = (role) => {
    return keycloak.tokenParsed?.realm_access?.roles?.includes(role);
  };

  return (
    <Router>
      <div className="min-h-screen bg-gray-100 flex">
        
        <aside className="w-64 bg-gray-800 text-white flex flex-col">
          <div className="p-4 bg-blue-700 font-bold text-lg text-center shadow-md">
            SIGA IPT
          </div>
          <nav className="flex-1 p-4 space-y-2">
            <Link to="/" className="block py-2 px-4 rounded hover:bg-gray-700 transition">Inicio</Link>
            
            {hasRole('STUDENT') && (
              <>
                <Link to="/mi-perfil" className="block py-2 px-4 rounded hover:bg-gray-700 transition">Mi Perfil</Link>
                <Link to="/estudiante/inscripcion" className="block py-2 px-4 rounded hover:bg-gray-700 transition">Inscripción (Épica 6)</Link>
                <Link to="/estudiante/mi-horario" className="block py-2 px-4 rounded hover:bg-gray-700 transition">Mi Carga Académica</Link>
              </>
            )}

            {hasRole('ADMIN') && (
              <>
                <Link to="/admin/periodos" className="block py-2 px-4 rounded hover:bg-gray-700 transition">Gestión Académica</Link>
                <Link to="/admin/matricula" className="block py-2 px-4 rounded hover:bg-gray-700 transition">Matricular Estudiante</Link>
              </>
            )}

            {hasRole('TEACHER') && (
              <>
                <Link to="/profesor/calificaciones" className="block py-2 px-4 rounded hover:bg-gray-700 transition">Mis Calificaciones</Link>
              </>
            )}
          </nav>
        </aside>

        <div className="flex-1 flex flex-col">
          <header className="bg-white shadow-sm p-4 flex justify-between items-center">
            <h1 className="text-xl font-semibold text-gray-700">Sistema Académico</h1>
            <div className="flex items-center gap-4">
              <span className="text-sm font-medium text-gray-600">
                Usuario: {keycloak.tokenParsed?.preferred_username}
              </span>
              <button 
                onClick={handleLogout} 
                className="bg-red-500 text-white px-4 py-2 rounded text-sm font-bold hover:bg-red-600 transition"
              >
                Cerrar Sesión
              </button>
            </div>
          </header>

          <main className="p-8">
            <Routes>
              <Route path="/" element={
                <div className="bg-white p-6 rounded shadow">
                  <h2 className="text-2xl font-semibold mb-2">Bienvenido a SIGA IPT</h2>
                  <p className="text-gray-600">Selecciona una opción del menú lateral.</p>
                </div>
              } />
              <Route path="/mi-perfil" element={<StudentProfile />} />
              <Route path="/admin/periodos" element={<AdminPanel />} />
              <Route path="/admin/matricula" element={<MatriculationPanel />} />
              <Route path="/estudiante/inscripcion" element={<EnrollmentPanel />} />
              <Route path="/estudiante/mi-horario" element={<StudentSchedulePanel />} />
              <Route path="/profesor/calificaciones" element={<TeacherPanel />} />
            </Routes>
          </main>
        </div>

      </div>
    </Router>
  );
}

export default App;