# RadAI Frontend

Angular 17+ frontend for the RadAI mental health companion application.

## Prerequisites

- Node.js 18+ 
- npm 9+

## Setup

1. Install dependencies:
```bash
cd Frontend
npm install
```

2. Start development server:
```bash
npm start
```

The app will be available at `http://localhost:4200`

## Features

- **Authentication**: Login, Register, and Anonymous sessions
- **Chat Interface**: Real-time conversation with emotion selection
- **Emotion Tracking**: Select from 10 different emotions with intensity slider
- **Bilingual Support**: English and Malay
- **Text-to-Speech**: Listen to bot responses
- **Dashboard**: View emotion trends and history
- **AI Toggle**: Switch between AI-enhanced and basic responses

## Project Structure

```
src/
├── app/
│   ├── components/       # Shared components
│   │   └── navbar/
│   ├── guards/           # Route guards
│   ├── interceptors/     # HTTP interceptors
│   ├── models/           # TypeScript interfaces
│   ├── pages/            # Page components
│   │   ├── chat/
│   │   ├── dashboard/
│   │   ├── login/
│   │   └── register/
│   ├── services/         # API services
│   ├── app.component.ts
│   ├── app.config.ts
│   └── app.routes.ts
├── environments/         # Environment configs
└── styles.scss           # Global styles
```

## API Integration

The frontend connects to the Spring Boot backend at:
- Development: `http://localhost:8080/api`
- Production: `/api` (same origin)

### Key Endpoints Used

- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `POST /api/auth/anonymous` - Anonymous session
- `POST /api/chat/flow/process` - Basic chat
- `POST /api/chat/flow/process-with-ai` - AI-enhanced chat
- `GET /api/chat/flow/dashboard` - Dashboard data

## Build for Production

```bash
npm run build
```

Output will be in `dist/radai-frontend/`

## Styling

- Bootstrap 5.3 for layout and components
- Bootstrap Icons for iconography
- Custom SCSS with dark theme using CSS variables

## Configuration

Edit `src/environments/environment.ts` to change API URL:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```
