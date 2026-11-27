import { EVENT_STATUSES, isSection, type LayoutNode } from './models';

describe('models', () => {
  it('lists every event status once', () => {
    expect(EVENT_STATUSES).toEqual(['DRAFT', 'PUBLISHED', 'ON_SALE', 'SOLD_OUT', 'FINISHED']);
  });

  describe('isSection', () => {
    const block: LayoutNode = { name: 'Block A', rows: [{ label: 'A', seatCount: 10 }] };
    const section: LayoutNode = { name: 'Floor', children: [block] };

    it('detects a section by its children', () => {
      expect(isSection(section)).toBe(true);
    });

    it('treats a node with rows as a seat block', () => {
      expect(isSection(block)).toBe(false);
    });

    it('narrows the type', () => {
      const names = isSection(section) ? section.children.map((child) => child.name) : [];
      expect(names).toEqual(['Block A']);
    });
  });
});
