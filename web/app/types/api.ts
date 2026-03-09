export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  username: string
  role: "ADMIN" | "TEACHER" | "STUDENT"
}

export interface StudentResponse {
  id: number
  firstName: string
  lastName: string
  email: string
  phoneNumber: string
  dateOfBirth: string
  enrollmentDate: string
  status: "ACTIVE" | "INACTIVE" | "GRADUATED" | "SUSPENDED"
  studentId: string
}

export interface TeacherResponse {
  id: number
  firstName: string
  lastName: string
  email: string
  phoneNumber: string
  department: string
  employeeId: string
  employmentType: "FULL_TIME" | "PART_TIME" | "CONTRACT"
  status: "ACTIVE" | "INACTIVE"
  assignedModuleIds: string[]
  hireDate: string
}

export interface ModuleResponse {
  id: string
  moduleCode: string
  moduleName: string
  description: string
  credits: number
  semester: string
  maxStudents: number
  currentEnrollment: number
  status: "ACTIVE" | "INACTIVE" | "COMPLETED"
  teacherId: number
}

export interface GradeResponse {
  id: number
  studentId: number
  moduleId: string
  teacherId: number
  score: string
  maxScore: string
  percentage: string
  grade: string
  assessmentType: string
  remarks: string
  semester: string
  academicYear: string
  createdAt: string
  updatedAt: string
}

export interface EnrollmentResponse {
  id: number
  studentId: number
  moduleId: string
  enrollmentDate: string
  status: "ENROLLED" | "COMPLETED" | "DROPPED" | "FAILED"
  dropDate: string | null
  dropReason: string | null
}

export interface NotificationResponse {
  id: number
  recipientId: number
  recipientEmail: string
  subject: string
  message: string
  type: "EMAIL" | "SMS" | "PUSH"
  status: "SENT" | "PENDING" | "FAILED"
  sentAt: string
}

export interface Session {
  token: string
  username: string
  role: "ADMIN" | "TEACHER" | "STUDENT"
  userId?: number
}
