import { AbstractChatProvider, XRequest, XRequestOptions } from '@ant-design/x-sdk';
import { ChatMessage, ThoughtItem, RetrievalTreeNode, TokenUsageReport } from '../types';
import { 
    EditOutlined, 
    SearchOutlined, 
    BranchesOutlined, 
    SortAscendingOutlined, 
    CheckCircleOutlined, 
    LoadingOutlined,
    CloseCircleOutlined
} from '@ant-design/icons';
import { message } from 'antd';
import React from 'react';

interface ChatInput {
  messages: ChatMessage[];
  kbId?: string;
  ragMethod?: string;
  ragParams?: any;
}

interface ChatOutput {
  event: string;
  data: any;
}

const getIcon = (name: string) => {
    switch(name) {
        case 'edit': return React.createElement(EditOutlined);
        case 'search': return React.createElement(SearchOutlined);
        case 'merge': return React.createElement(BranchesOutlined);
        case 'sort': return React.createElement(SortAscendingOutlined);
        case 'check': return React.createElement(CheckCircleOutlined);
        case 'error': return React.createElement(CloseCircleOutlined, { style: { color: '#ff4d4f' } });
        default: return React.createElement(LoadingOutlined);
    }
};

export class SmartRAGChatProvider extends AbstractChatProvider<ChatMessage, ChatInput, ChatOutput> {
  constructor(token?: string, endpoint: string = '/api/chat/test/full-rag-emitter') {
    super({
      request: XRequest(endpoint, {
        manual: true,
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      })
    });
  }

  transformParams(
    requestParams: Partial<ChatInput>,
    _options: XRequestOptions<ChatInput, ChatOutput>
  ): ChatInput {
    const { messages, ragParams } = requestParams;
    const history = messages?.slice(0, -1).map(m => ({ role: m.role, content: m.content }));

    return {
        ...ragParams,
        history
    } as any;
  }

  transformLocalMessage(requestParams: Partial<ChatInput>): ChatMessage {
    const { messages } = requestParams;
    return messages?.[messages.length - 1] || {
      id: Date.now().toString(),
      role: 'user',
      content: '',
      createTime: Date.now()
    };
  }

  transformMessage(info: {
    originMessage?: ChatMessage;
    chunk: any;
    chunks: any[];
    status: 'local' | 'loading' | 'updating' | 'success' | 'error' | 'abort';
  }): ChatMessage {
    const { originMessage, chunk, status } = info;
    
    const currentMessage: ChatMessage = originMessage || {
      id: Date.now().toString(),
      role: 'ai',
      content: '',
      thoughts: [],
      createTime: Date.now(),
      status: 'loading'
    };

    // Handle final success state
    if (status === 'success') {
        return {
            ...currentMessage,
            status: 'success',
            // Ensure the last thought is marked as success if it exists
            thoughts: currentMessage.thoughts?.map(t => {
                if (t.status === 'processing') {
                    const startTime = (t as any).startTime || Date.now();
                    return { 
                        ...t, 
                        status: 'success',
                        duration: Date.now() - startTime
                    };
                }
                return t;
            })
        };
    }

    if (!chunk) return currentMessage;

    // Handle SSE chunk from XRequest
    let { event, data } = chunk;

    // Parse data if it's a JSON string
    if (typeof data === 'string') {
        try {
            // Try to parse if it looks like JSON, otherwise treat as raw string
            if (data.trim().startsWith('{') || data.trim().startsWith('[')) {
                 const parsed = JSON.parse(data);
                 data = parsed;
            }
        } catch (e) {
            // ignore
        }
    }
    
    // Ensure data is a string for concatenation if it's not an object
    const textData = typeof data === 'string' ? data : (data?.delta || JSON.stringify(data));

    switch (event) {
      case 'thought':
        const thoughts = currentMessage.thoughts || [];
        const lastThought = thoughts[thoughts.length - 1];
        
        // Logic optimization: If receiving a new thought, mark the previous one as success
        let updatedThoughts = [...thoughts];
        if (lastThought && lastThought.status === 'processing') {
             const endTime = Date.now();
             const startTime = (lastThought as any).startTime || (endTime - 1000);
             updatedThoughts[updatedThoughts.length - 1] = {
                 ...lastThought,
                 status: 'success',
                 duration: endTime - startTime
             };
        }

        const thoughtStatus = data.status || 'processing';
        
        if (thoughtStatus === 'error') {
            message.error(data.content || '处理过程中发生错误');
        }

        // Create new thought
        // Backend sends: { status, content, icon }
        // "content" is the human-readable step description shown as the title.
        const newThought: ThoughtItem & { startTime: number } = {
            title: data.content || data.title || (thoughtStatus === 'error' ? '发生错误' : '思考中...'),
            status: thoughtStatus,
            content: '',
            icon: getIcon(data.icon || (thoughtStatus === 'error' ? 'error' : undefined)),
            duration: 0,
            startTime: Date.now()
        };
        
        return { 
            ...currentMessage, 
            thoughts: [...updatedThoughts, newThought],
            status: thoughtStatus === 'error' ? 'error' : 'updating'
        };

      case 'retrieval_tree':
        return {
            ...currentMessage,
            retrievalTree: Array.isArray(data) ? (data as RetrievalTreeNode[]) : [],
            status: 'updating',
        };

      case 'token_usage':
        return {
            ...currentMessage,
            tokenUsage: data as TokenUsageReport,
            status: 'updating',
        };

      case 'ref':
        // Handle references
        return {
            ...currentMessage,
            references: Array.isArray(data) ? data : [],
            status: 'updating'
        };

      case 'message':
        // If we receive a message, ensure the last thought is finished
        let currentThoughts = currentMessage.thoughts || [];
        const lastProcessingThought = currentThoughts[currentThoughts.length - 1];
        if (lastProcessingThought && lastProcessingThought.status === 'processing') {
             const endTime = Date.now();
             const startTime = (lastProcessingThought as any).startTime || (endTime - 1000);
             const finishedThought = { 
                 ...lastProcessingThought, 
                 status: 'success' as const,
                 duration: endTime - startTime
             };
             currentThoughts = [...currentThoughts.slice(0, -1), finishedThought];
        }

        return {
          ...currentMessage,
          thoughts: currentThoughts,
          content: (currentMessage.content || '') + textData,
          status: 'updating'
        };
      
      case 'done':
        return {
            ...currentMessage,
            status: 'success',
            // Finalize any remaining processing thoughts
            thoughts: currentMessage.thoughts?.map(t => {
                if (t.status === 'processing') {
                    const startTime = (t as any).startTime || Date.now();
                    return { ...t, status: 'success' as const, duration: Date.now() - startTime };
                }
                return t;
            })
        };
        
      default:
        return currentMessage;
    }
  }
}
