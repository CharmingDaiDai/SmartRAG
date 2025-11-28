# SmartDoc Frontend Project Guide

## 1. Project Overview
SmartDoc is a RAG (Retrieval-Augmented Generation) based knowledge base system. The frontend is built with React, TypeScript, and Ant Design, featuring a modern chat interface powered by Ant Design X.

### Tech Stack
- **Framework**: React 18 + Vite
- **Language**: TypeScript
- **UI Library**: Ant Design (v5)
- **Chat UI**: Ant Design X (@ant-design/x)
- **State Management**: Zustand
- **Routing**: React Router DOM (v6)
- **Styling**: Tailwind CSS + CSS Modules
- **Markdown**: @ant-design/x-markdown (with plugins for Code, Latex, Mermaid)

## 2. Directory Structure

```
src/
├── components/         # Reusable UI components
│   ├── common/        # Generic components (Loading, Error, etc.)
│   ├── rag/           # RAG-specific components (ThoughtChain, etc.)
│   └── ReferenceViewer.tsx # Document reference display component
├── config/            # Configuration files
├── layouts/           # Page layouts
│   └── BasicLayout.tsx # Main application layout with sidebar and header
├── pages/             # Route components
│   ├── auth/          # Login/Register pages
│   ├── Chat/          # Main RAG Chat interface
│   ├── TestChat/      # Standalone Chat UI for testing/debugging
│   ├── Dashboard/     # System dashboard
│   ├── KnowledgeBase/ # KB management
│   └── documents/     # Document management
├── services/          # API client services (Axios based)
│   ├── api.ts         # Axios instance and interceptors
│   ├── kbService.ts   # Knowledge Base APIs
│   └── ...
├── store/             # Global state (Zustand)
│   └── useAppStore.ts # Main store (User, Theme, KB state)
├── types/             # TypeScript type definitions
│   └── index.ts       # Shared interfaces (User, ChatMessage, etc.)
├── utils/             # Helper functions
└── App.tsx            # Root component with Routing and Theme Provider
```

## 3. Key Features & Implementation Details

### 3.1 RAG Chat Interface (`src/pages/Chat`)
- **Core**: Uses `useXChat` from `@ant-design/x-sdk` for message management.
- **Rendering**: Uses `XMarkdown` to render AI responses.
- **Plugins**:
  - `HighlightCode`: Syntax highlighting for code blocks.
  - `Latex`: Math formula rendering.
  - `Mermaid`: Diagram rendering.
- **Streaming**: Supports typewriter effect for streaming responses.
- **Custom Components**:
  - `AnimatedThoughtChain`: Displays the AI's reasoning process (CoT).
  - `ReferenceViewer`: Shows cited documents with relevance scores.

### 3.2 Theme System (Light/Dark Mode)
- **State**: Managed in `useAppStore.ts` (`themeMode`).
- **Persistence**: Saved to `localStorage`.
- **Implementation**:
  - `App.tsx`: Uses `ConfigProvider` to switch Ant Design algorithms (`defaultAlgorithm` vs `darkAlgorithm`).
  - `index.css`: Uses CSS variables and `[data-theme='dark']` selectors for custom overrides (scrollbars, markdown styles).
  - `BasicLayout.tsx`: Contains the toggle button in the header.

### 3.3 Markdown Rendering
- **Configuration**: Located in `Chat/index.tsx` and `TestChat/index.tsx`.
- **Styling**:
  - Imports `@ant-design/x-markdown/themes/{light,dark}.css`.
  - Custom CSS in `index.css` to match GitHub-like code block styles.
  - `Code` component wrapper to handle syntax highlighting dynamically.

## 4. How to Extend

### Adding a New Page
1. Create the component in `src/pages/NewPage/index.tsx`.
2. Add the route in `src/App.tsx` inside the `ProtectedRoute` wrapper.
3. Add the menu item in `src/layouts/BasicLayout.tsx`.

### Modifying Chat Logic
- **Message Handling**: Edit `handleRequest` in `Chat/index.tsx`.
- **UI Customization**: Modify `Bubble.List` items configuration.
- **New Markdown Plugin**:
  1. Import plugin from `@ant-design/x-markdown/plugins`.
  2. Add to `MD_PLUGINS` array.
  3. If it requires a custom component, add to `MD_COMPONENTS`.

### API Integration
1. Define types in `src/types/index.ts`.
2. Add service methods in `src/services/`.
3. Call service in components using `useEffect` or event handlers.

## 5. Maintenance Notes
- **Type Safety**: Avoid `any`. Use interfaces defined in `src/types`.
- **Performance**: Use `useMemo` for expensive calculations like message list rendering.
- **Dark Mode**: Always test new UI components in both light and dark modes. Use `token.colorBgContainer` from Ant Design theme token instead of hardcoded colors.
