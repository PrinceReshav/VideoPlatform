import { Dialog } from '../components/ui/Dialog.tsx';
import { Users } from 'lucide-react';
import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { User, Search, Shield, MoreHorizontal, Ban, UserCheck, UserX, Mail } from 'lucide-react';
import { Card } from '../components/ui/Card.tsx';
import { Button } from '../components/ui/Button.tsx';
import { Input } from '../components/ui/Input.tsx';
import { Select } from '../components/ui/Select.tsx';
import { Badge } from '../components/ui/Badge.tsx';
import { authApi } from '../services/api/authApi.ts';
import { adminApi } from '../services/api/adminApi.ts';

export function AdminUsersPage() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = React.useState('');
  const [roleFilter, setRoleFilter] = React.useState<string>('all');
  const [statusFilter, setStatusFilter] = React.useState<string>('all');
  const [page, setPage] = React.useState(0);
  const [selectedUser, setSelectedUser] = React.useState<any | null>(null);
  const [actionDialog, setActionDialog] = React.useState<'ban' | 'unban' | 'role' | null>(null);

  const { data: users, isLoading } = useQuery({
    queryKey: ['adminUsers', searchQuery, roleFilter, statusFilter, page],
    queryFn: () => adminApi.getUsers({ search: searchQuery, role: roleFilter, status: statusFilter, page, size: 20 }),
  });

  const banMutation = useMutation({
    mutationFn: ({ userId, reason }: { userId: string; reason: string }) => adminApi.banUser(userId, reason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['adminUsers'] }),
  });

  const unbanMutation = useMutation({
    mutationFn: (userId: string) => adminApi.unbanUser(userId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['adminUsers'] }),
  });

  const roleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: string }) => adminApi.updateUserRole(userId, role),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['adminUsers'] }),
  });

  const handleAction = (action: 'ban' | 'unban' | 'role', user: any) => {
    setSelectedUser(user);
    setActionDialog(action);
  };

  return (
    <div className="max-w-7xl mx-auto p-4 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)] flex items-center gap-3">
            <Shield className="w-8 h-8 text-[var(--color-error)]" />
            User Management
          </h1>
          <p className="text-[var(--color-text-secondary)]">Manage platform users and permissions</p>
        </div>
      </div>

      {/* Filters */}
      <Card className="p-4">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1 min-w-[250px]">
            <Input
              placeholder="Search users..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              leftIcon={<Search className="w-4 h-4" />}
            />
          </div>
          <Select value={roleFilter} onValueChange={setRoleFilter} className="w-48">
            <Select.Trigger><Select.Value placeholder="All Roles" /></Select.Trigger>
            <Select.Content>
              <Select.Item value="all">All Roles</Select.Item>
              <Select.Item value="USER">Viewer</Select.Item>
              <Select.Item value="CREATOR">Creator</Select.Item>
              <Select.Item value="ADMIN">Admin</Select.Item>
              <Select.Item value="MODERATOR">Moderator</Select.Item>
            </Select.Content>
          </Select>
          <Select value={statusFilter} onValueChange={setStatusFilter} className="w-48">
            <Select.Trigger><Select.Value placeholder="All Status" /></Select.Trigger>
            <Select.Content>
              <Select.Item value="all">All Status</Select.Item>
              <Select.Item value="active">Active</Select.Item>
              <Select.Item value="banned">Banned</Select.Item>
              <Select.Item value="locked">Locked</Select.Item>
            </Select.Content>
          </Select>
        </div>
      </Card>

      {/* Users Table */}
      <Card>
        {isLoading ? (
          <div className="p-8 space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="animate-pulse flex items-center gap-4">
                <div className="w-10 h-10 rounded-full bg-[var(--color-bg-tertiary)]" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-1/4 bg-[var(--color-bg-tertiary)] rounded" />
                  <div className="h-3 w-1/3 bg-[var(--color-bg-tertiary)] rounded" />
                </div>
                <div className="w-32 h-8 bg-[var(--color-bg-tertiary)] rounded" />
              </div>
            ))}
          </div>
        ) : users?.content?.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-[var(--color-border)] text-left text-sm font-medium text-[var(--color-text-secondary)]">
                    <th className="pb-3 pr-4">User</th>
                    <th className="pb-3 pr-4">Email</th>
                    <th className="pb-3 pr-4">Roles</th>
                    <th className="pb-3 pr-4">Status</th>
                    <th className="pb-3 pr-4">Joined</th>
                    <th className="pb-3 pr-4">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.content.map((user: any) => (
                    <tr key={user.id} className="border-b border-[var(--color-border)]/50 hover:bg-[var(--color-bg-hover)]/50">
                      <td className="py-4 pr-4">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-[var(--color-primary)]/10 flex items-center justify-center">
                            <User className="w-5 h-5 text-[var(--color-primary)]" />
                          </div>
                          <div>
                            <p className="font-medium text-[var(--color-text-primary)]">{user.displayName}</p>
                            <p className="text-xs text-[var(--color-text-secondary)]">@{user.username}</p>
                          </div>
                        </div>
                      </td>
                      <td className="py-4 pr-4 text-[var(--color-text-secondary)]">{user.email}</td>
                      <td className="py-4 pr-4">
                        <div className="flex flex-wrap gap-1">
                          {user.roles?.map((role: string) => (
                            <Badge key={role} variant={role === 'ADMIN' ? 'error' : role === 'CREATOR' ? 'success' : 'default'}>
                              {role}
                            </Badge>
                          ))}
                        </div>
                      </td>
                      <td className="py-4 pr-4">
                        <Badge variant={
                          user.blocked ? 'error' :
                          user.lockedUntil ? 'warning' :
                          'success'
                        }>
                          {user.blocked ? 'Banned' : user.lockedUntil ? 'Locked' : 'Active'}
                        </Badge>
                      </td>
                      <td className="py-4 pr-4 text-[var(--color-text-secondary)] text-sm">
                        {new Date(user.createdAt).toLocaleDateString()}
                      </td>
                      <td className="py-4 pr-4">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => handleAction(user.blocked ? 'unban' : 'ban', user)}
                            className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors text-[var(--color-text-secondary)] hover:text-[var(--color-error)]"
                            title={user.blocked ? 'Unban' : 'Ban'}
                          >
                            {user.blocked ? <UserCheck className="w-4 h-4" /> : <Ban className="w-4 h-4" />}
                          </button>
                          <button
                            onClick={() => handleAction('role', user)}
                            className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors text-[var(--color-text-secondary)] hover:text-[var(--color-primary)]"
                            title="Change Role"
                          >
                            <Shield className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {users.totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 p-4 border-t border-[var(--color-border)]">
                <Button variant="outline" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                  Previous
                </Button>
                <span className="px-4 text-[var(--color-text-secondary)]">
                  Page {page + 1} of {users.totalPages}
                </span>
                <Button variant="outline" disabled={page >= users.totalPages - 1} onClick={() => setPage(p => p + 1)}>
                  Next
                </Button>
              </div>
            )}
          </>
        ) : (
          <div className="p-8 text-center">
            <User className="w-16 h-16 mx-auto text-[var(--color-text-tertiary)] mb-4 opacity-50" />
            <h3 className="text-lg font-medium text-[var(--color-text-primary)] mb-2">No users found</h3>
            <p className="text-[var(--color-text-secondary)]">Try adjusting your search or filters</p>
          </div>
        )}
      </Card>

      {/* Action Dialogs */}
      {actionDialog === 'ban' && selectedUser && (
        <BanDialog user={selectedUser} onClose={() => { setActionDialog(null); setSelectedUser(null); }} onConfirm={(reason) => banMutation.mutate({ userId: selectedUser.id, reason })} />
      )}
      {actionDialog === 'unban' && selectedUser && (
        <ConfirmDialog
          title="Unban User"
          message={`Are you sure you want to unban ${selectedUser.displayName}?`}
          onClose={() => { setActionDialog(null); setSelectedUser(null); }}
          onConfirm={() => unbanMutation.mutate(selectedUser.id)}
        />
      )}
      {actionDialog === 'role' && selectedUser && (
        <RoleDialog user={selectedUser} onClose={() => { setActionDialog(null); setSelectedUser(null); }} onConfirm={(role) => roleMutation.mutate({ userId: selectedUser.id, role })} />
      )}
    </div>
  );
}

function BanDialog({ user, onClose, onConfirm }: { user: any; onClose: () => void; onConfirm: (reason: string) => void }) {
  const [reason, setReason] = React.useState('');
  return (
    <Dialog open onOpenChange={onClose}>
      <Dialog.Content>
        <Dialog.Header>
          <Dialog.Title>Ban User</Dialog.Title>
          <Dialog.Description>Enter reason for banning {user.displayName}</Dialog.Description>
        </Dialog.Header>
        <form onSubmit={(e) => { e.preventDefault(); onConfirm(reason); onClose(); }} className="space-y-4">
          <Input label="Reason" value={reason} onChange={(e) => setReason(e.target.value)} as="textarea" rows={3} required />
          <Dialog.Footer>
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" variant="danger">Ban User</Button>
          </Dialog.Footer>
        </form>
      </Dialog.Content>
    </Dialog>
  );
}

function ConfirmDialog({ title, message, onClose, onConfirm }: { title: string; message: string; onClose: () => void; onConfirm: () => void }) {
  return (
    <Dialog open onOpenChange={onClose}>
      <Dialog.Content>
        <Dialog.Header>
          <Dialog.Title>{title}</Dialog.Title>
          <Dialog.Description>{message}</Dialog.Description>
        </Dialog.Header>
        <Dialog.Footer>
          <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
          <Button type="button" variant="danger" onClick={() => { onConfirm(); onClose(); }}>Confirm</Button>
        </Dialog.Footer>
      </Dialog.Content>
    </Dialog>
  );
}

function RoleDialog({ user, onClose, onConfirm }: { user: any; onClose: () => void; onConfirm: (role: string) => void }) {
  const [role, setRole] = React.useState(user.roles?.[0] || 'USER');
  return (
    <Dialog open onOpenChange={onClose}>
      <Dialog.Content>
        <Dialog.Header>
          <Dialog.Title>Change Role</Dialog.Title>
          <Dialog.Description>Select new role for {user.displayName}</Dialog.Description>
        </Dialog.Header>
        <Select value={role} onValueChange={setRole}>
          <Select.Trigger><Select.Value /></Select.Trigger>
          <Select.Content>
            <Select.Item value="USER">Viewer</Select.Item>
            <Select.Item value="CREATOR">Creator</Select.Item>
            <Select.Item value="MODERATOR">Moderator</Select.Item>
            <Select.Item value="ADMIN">Admin</Select.Item>
          </Select.Content>
        </Select>
        <Dialog.Footer>
          <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
          <Button type="button" onClick={() => { onConfirm(role); onClose(); }}>Update Role</Button>
        </Dialog.Footer>
      </Dialog.Content>
    </Dialog>
  );
}