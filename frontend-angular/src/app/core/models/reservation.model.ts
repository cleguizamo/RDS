export interface Reservation {
  id: number;
  date: string;
  time: string;
  numberOfPeople: number;
  status: boolean;
  notes?: string;
  userId: number;
  userName: string;
  userEmail: string;
}

export interface ReservationRequest {
  date: string;
  time: string;
  numberOfPeople: number;
  notes?: string;
}
