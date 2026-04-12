import React from 'react';
import { Popover, Segmented, theme, Typography, Tooltip, Button } from 'antd';
import { SettingOutlined } from '@ant-design/icons';
import { useAppStore } from '../store/useAppStore';
import {
  DEFAULT_PERSONALIZATION,
  FONT_SIZES,
  type FontSize,
} from '../config/themeConfig';

const { Text } = Typography;

function SectionLabel({ children }: { children: React.ReactNode }) {
  const { token } = theme.useToken();
  return (
    <Text style={{
      fontSize: 11,
      fontWeight: 600,
      color: token.colorTextTertiary,
      letterSpacing: '0.06em',
    }}>
      {children}
    </Text>
  );
}

function PopoverContent() {
  const {
    themeMode, toggleTheme,
    fontSize, setFontSize,
    setColorTheme,
    setFontFamily,
    setUIStyle,
  } = useAppStore();
  const { token } = theme.useToken();

  return (
    <div style={{ width: 260, display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* 外观模式 */}
      <div>
        <SectionLabel>外观模式</SectionLabel>
        <div style={{ marginTop: 8 }}>
          <Segmented
            block
            value={themeMode}
            onChange={(val) => { if (val !== themeMode) toggleTheme(); }}
            options={[
              { label: '浅色', value: 'light' },
              { label: '深色', value: 'dark' },
            ]}
          />
        </div>
      </div>

      {/* 视觉语言锁定说明 */}
      <div>
        <SectionLabel>界面风格</SectionLabel>
        <Text style={{ marginTop: 8, display: 'block', fontSize: 12, color: token.colorTextSecondary, lineHeight: 1.6 }}>
          当前已统一为极简高级视觉体系，避免多风格混用造成观感割裂。
        </Text>
      </div>

      {/* 字号 */}
      <div>
        <SectionLabel>字号</SectionLabel>
        <div style={{ marginTop: 8 }}>
          <Segmented
            block
            value={fontSize}
            onChange={(val) => setFontSize(val as FontSize)}
            options={Object.values(FONT_SIZES).map((s) => ({
              label: s.label,
              value: s.key,
            }))}
          />
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <Button
          size="small"
          onClick={() => {
            if (themeMode !== 'light') {
              toggleTheme();
            }
            setColorTheme(DEFAULT_PERSONALIZATION.colorTheme);
            setFontFamily(DEFAULT_PERSONALIZATION.fontFamily);
            setFontSize(DEFAULT_PERSONALIZATION.fontSize);
            setUIStyle(DEFAULT_PERSONALIZATION.uiStyle);
          }}
        >
          恢复默认设置
        </Button>
      </div>
    </div>
  );
}

export default function ThemePopover() {
  const { token } = theme.useToken();

  return (
    <Popover
      content={<PopoverContent />}
      title={
        <Text style={{ fontSize: 13, fontWeight: 600 }}>个性化设置</Text>
      }
      trigger="click"
      placement="bottomRight"
      arrow={{ pointAtCenter: true }}
      overlayInnerStyle={{ padding: '14px 18px 18px' }}
    >
      <Tooltip title="个性化设置" mouseEnterDelay={0.5}>
        <Button
          type="text"
          aria-label="打开个性化设置"
          style={{
            width: 36,
            height: 36,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: token.colorTextSecondary,
            borderRadius: 8,
          }}
          icon={<SettingOutlined style={{ fontSize: 16 }} />}
        />
      </Tooltip>
    </Popover>
  );
}
