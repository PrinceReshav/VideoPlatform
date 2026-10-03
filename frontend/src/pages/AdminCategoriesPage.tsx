import { Dialog } from '../components/ui/Dialog.tsx';
import { Globe } from 'lucide-react';
import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Search, Folder, Tag, MoreHorizontal, Edit, Trash2, ChevronRight } from 'lucide-react';
import { Card } from '../components/ui/Card.tsx';
import { Button } from '../components/ui/Button.tsx';
import { Input } from '../components/ui/Input.tsx';
import { Select } from '../components/ui/Select.tsx';
import { categoryApi } from '../services/api/categoryApi.ts';
import { genreApi } from '../services/api/genreApi.ts';

export function AdminCategoriesPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = React.useState<'categories' | 'genres' | 'languages'>('categories');
  const [showCreateDialog, setShowCreateDialog] = React.useState(false);
  const [editingItem, setEditingItem] = React.useState<any | null>(null);
  const [formData, setFormData] = React.useState({
    name: '',
    description: '',
    parentId: '',
    sortOrder: 0,
    icon: '',
    color: '',
  });

  const { data: categories } = useQuery({
    queryKey: ['adminCategories'],
    queryFn: () => categoryApi.getAllCategories(),
  });

  const { data: genres } = useQuery({
    queryKey: ['adminGenres'],
    queryFn: () => genreApi.getAllGenres(),
  });

  const createCategoryMutation = useMutation({
    mutationFn: (data: any) => categoryApi.createCategory(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminCategories'] });
      setShowCreateDialog(false);
      setFormData({ name: '', description: '', parentId: '', sortOrder: 0, icon: '', color: '' });
    },
  });

  const updateCategoryMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) => categoryApi.updateCategory(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminCategories'] });
      setEditingItem(null);
    },
  });

  const deleteCategoryMutation = useMutation({
    mutationFn: (id: string) => categoryApi.deleteCategory(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['adminCategories'] }),
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editingItem) {
      updateCategoryMutation.mutate({ id: editingItem.id, data: formData });
    } else {
      createCategoryMutation.mutate(formData);
    }
  };

  const handleEdit = (item: any) => {
    setEditingItem(item);
    setFormData({
      name: item.name,
      description: item.description || '',
      parentId: item.parentId || '',
      sortOrder: item.sortOrder || 0,
      icon: item.icon || '',
      color: item.color || '',
    });
    setShowCreateDialog(true);
  };

  const handleDelete = (id: string) => {
    if (window.confirm('Are you sure you want to delete this? This cannot be undone.')) {
      deleteCategoryMutation.mutate(id);
    }
  };

  const renderTree = (items: any[], level = 0) => {
    return items.map((item: any) => (
      <div key={item.id} className={level > 0 ? 'ml-8' : ''}>
        <div className="flex items-center gap-3 py-2 px-3 hover:bg-[var(--color-bg-hover)] rounded-lg">
          <div className="flex-1 min-w-0">
            <p className="font-medium text-[var(--color-text-primary)] truncate">{item.name}</p>
            <p className="text-xs text-[var(--color-text-secondary)]">Slug: {item.slug} {item.isSystem && '• System'}</p>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="icon" onClick={() => handleEdit(item)} title="Edit">
              <Edit className="w-4 h-4" />
            </Button>
            <Button variant="ghost" size="icon" onClick={() => handleDelete(item.id)} title="Delete" className="text-[var(--color-error)] hover:text-[var(--color-error)]">
              <Trash2 className="w-4 h-4" />
            </Button>
          </div>
        </div>
        {item.children && item.children.length > 0 && renderTree(item.children, level + 1)}
      </div>
    ));
  };

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)] flex items-center gap-3">
            <Folder className="w-8 h-8 text-[var(--color-primary)]" />
            Taxonomy Management
          </h1>
          <p className="text-[var(--color-text-secondary)]">Manage categories, genres, and languages</p>
        </div>
        <Button onClick={() => { setEditingItem(null); setFormData({ name: '', description: '', parentId: '', sortOrder: 0, icon: '', color: '' }); setShowCreateDialog(true); }}>
          <Plus className="w-4 h-4" />
          Add {activeTab === 'categories' ? 'Category' : activeTab === 'genres' ? 'Genre' : 'Language'}
        </Button>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mb-6 border-b border-[var(--color-border)]">
        {[
          { id: 'categories', label: 'Categories', icon: Folder },
          { id: 'genres', label: 'Genres', icon: Tag },
          { id: 'languages', label: 'Languages', icon: Globe },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={`flex items-center gap-2 px-4 py-2 rounded-t-lg border-b-2 font-medium transition-colors ${
              activeTab === tab.id
                ? 'border-[var(--color-primary)] text-[var(--color-primary)]'
                : 'border-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
            }`}
          >
            <tab.icon className="w-4 h-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      <Card className="p-4">
        {activeTab === 'categories' && (
          <div className="space-y-2">
            {categories?.filter(c => !c.parentId).map((cat: any) => (
              <CategoryTreeItem key={cat.id} item={cat} onEdit={handleEdit} onDelete={handleDelete} allCategories={categories} />
            ))}
          </div>
        )}

        {activeTab === 'genres' && (
          <div className="space-y-2">
            {genres?.filter(g => !g.parentId).map((genre: any) => (
              <CategoryTreeItem key={genre.id} item={genre} onEdit={handleEdit} onDelete={handleDelete} allCategories={genres} />
            ))}
          </div>
        )}

        {activeTab === 'languages' && (
          <div className="space-y-2">
            {/* Languages would be rendered here */}
            <p className="text-[var(--color-text-secondary)]">Languages management coming soon...</p>
          </div>
        )}
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
        <Dialog.Content className="max-w-md">
          <Dialog.Header>
            <Dialog.Title>{editingItem ? 'Edit' : 'Create'} {activeTab === 'categories' ? 'Category' : activeTab === 'genres' ? 'Genre' : 'Language'}</Dialog.Title>
          </Dialog.Header>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input label="Name *" value={formData.name} onChange={(e) => setFormData({...formData, name: e.target.value})} required />
            <Input label="Description" value={formData.description} onChange={(e) => setFormData({...formData, description: e.target.value})} as="textarea" rows={2} />
            {activeTab !== 'languages' && (
              <>
                <Select label="Parent" value={formData.parentId} onChange={(e) => setFormData({...formData, parentId: e.target.value})}>
                  <Select.Trigger><Select.Value placeholder="None (root level)" /></Select.Trigger>
                  <Select.Content>
                    <Select.Item value="">None (root level)</Select.Item>
                    {(activeTab === 'categories' ? categories : genres)?.filter((c: any) => c.id !== editingItem?.id).map((c: any) => (
                      <Select.Item key={c.id} value={c.id}>{c.name}</Select.Item>
                    ))}
                  </Select.Content>
                </Select>
                <Input label="Sort Order" type="number" value={formData.sortOrder} onChange={(e) => setFormData({...formData, sortOrder: parseInt(e.target.value) || 0})} />
              </>
            )}
            {activeTab === 'categories' && (
              <>
                <Input label="Icon" value={formData.icon} onChange={(e) => setFormData({...formData, icon: e.target.value})} placeholder="🎬" />
                <Input label="Color" value={formData.color} onChange={(e) => setFormData({...formData, color: e.target.value})} placeholder="#6366f1" />
              </>
            )}
            <Dialog.Footer>
              <Button type="button" variant="outline" onClick={() => { setShowCreateDialog(false); setEditingItem(null); }}>Cancel</Button>
              <Button type="submit">{editingItem ? 'Update' : 'Create'}</Button>
            </Dialog.Footer>
          </form>
        </Dialog.Content>
      </Dialog>
    </div>
  );
}

function CategoryTreeItem({ item, onEdit, onDelete, allCategories }: { item: any; onEdit: (item: any) => void; onDelete: (id: string) => void; allCategories: any[] }) {
  return (
    <>
      <div className="flex items-center gap-3 py-2 px-3 hover:bg-[var(--color-bg-hover)] rounded-lg">
        {item.icon && <span className="text-lg">{item.icon}</span>}
        <div className="flex-1 min-w-0">
          <p className="font-medium text-[var(--color-text-primary)] truncate">{item.name}</p>
          <p className="text-xs text-[var(--color-text-secondary)]">Slug: {item.slug} {item.isSystem && '• System'}</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="icon" onClick={() => onEdit(item)} title="Edit">
            <Edit className="w-4 h-4" />
          </Button>
          {!item.isSystem && (
            <Button variant="ghost" size="icon" onClick={() => onDelete(item.id)} title="Delete" className="text-[var(--color-error)] hover:text-[var(--color-error)]">
              <Trash2 className="w-4 h-4" />
            </Button>
          )}
        </div>
      </div>
      {item.children && item.children.length > 0 && (
        <div className="ml-8 space-y-2 border-l border-[var(--color-border)] pl-4">
          {item.children.map((child: any) => (
            <CategoryTreeItem key={child.id} item={child} onEdit={onEdit} onDelete={onDelete} allCategories={allCategories} />
          ))}
        </div>
      )}
    </>
  );
}