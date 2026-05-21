/**
 * Dados de teste reutilizáveis para todos os specs Vitest.
 */

export const fixtureConsulta = (overrides: Record<string, unknown> = {}) => ({
  id: 1,
  patientId: 10,
  patientName: 'Lucas Paciente',
  professionalId: 2,
  professionalName: 'Dra. Ana Souza',
  startTime: '2026-06-01T09:00:00',
  reason: 'Consulta de rotina',
  status: 'AGENDADA',
  cancelReason: null,
  podeCancelar: true,
  atrasada: false,
  modalidadeAtendimento: 'PRESENCIAL',
  ...overrides,
});

export const fixtureConsultaAtrasada = (overrides: Record<string, unknown> = {}) =>
  fixtureConsulta({
    id: 99,
    startTime: '2026-01-10T09:00:00',
    status: 'AGENDADA',
    atrasada: true,
    ...overrides,
  });

export const fixtureProfissional = (overrides: Record<string, unknown> = {}) => ({
  id: 2,
  name: 'Dra. Ana Souza',
  email: 'ana@clinica.com',
  specialty: 'Psicologia',
  registroConselho: 'CRP-12345',
  tipoRegistro: 'CRP',
  modalidadeAtendimento: 'PRESENCIAL',
  ...overrides,
});

export const fixturePaciente = (overrides: Record<string, unknown> = {}) => ({
  id: 10,
  name: 'Lucas Paciente',
  email: 'lucas@email.com',
  cpf: '111.111.111-11',
  phone: '11999998888',
  active: true,
  infractionCount: 0,
  blockedUntil: null,
  ...overrides,
});

export const fixtureDashboardProfissional = (overrides: Record<string, unknown> = {}) => ({
  consultasHoje: 3,
  pendentesConfirmacao: 1,
  consultasAtrasadas: 0,
  totalPacientes: 20,
  ...overrides,
});

export const fixtureDocumento = (overrides: Record<string, unknown> = {}) => ({
  id: 5,
  titulo: 'Laudo de Psicologia',
  tipo: 'PDF',
  disponivel: true,
  criadoEm: '2026-05-01T10:00:00',
  nomeArquivo: 'laudo.pdf',
  ...overrides,
});
