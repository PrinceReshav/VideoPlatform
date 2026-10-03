import { Link } from 'react-router-dom';
import { Dialog } from '../components/ui/Dialog.tsx';
import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { FileText, Search, Shield, MoreHorizontal, Ban, Eye, Flag, Check, X, AlertTriangle, MessageCircle, User } from 'lucide-react';
import { Card } from '../components/ui/Card.tsx';
import { Button } from '../components/ui/Button.tsx';
import { Input } from '../components/ui/Input.tsx';
import { Select } from '../components/ui/Select.tsx';
import { Badge } from '../components/ui/Badge.tsx';
import { adminApi } from '../services/api/adminApi.ts';
import { formatDistanceToNow } from 'date-fns';

export function AdminReportsPage() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = React.useState('');
  const [statusFilter, setStatusFilter] = React.useState<string>('all');
  const [typeFilter, setTypeFilter] = React.useState<string>('all');
  const [page, setPage] = React.useState(0);
  const [selectedReport, setSelectedReport] = React.useState<any | null>(null);
  const [actionDialog, setActionDialog] = React.useState<'resolve' | 'dismiss' | 'escalate' | null>(null);

  const { data: reports, isLoading } = useQuery({
    queryKey: ['adminReports', searchQuery, statusFilter, typeFilter, page],
    queryFn: () => adminApi.getReports({ search: searchQuery, status: statusFilter, type: typeFilter, page, size: 20 }),
  });

  const resolveMutation = useMutation({
    mutationFn: ({ reportId, action, notes }: { reportId: string; action: string; notes: string }) =>
      adminApi.resolveReport(reportId, action, notes),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['adminReports'] }),
  });

  const handleAction = (action: 'resolve' | 'dismiss' | 'escalate', report: any) => {
    setSelectedReport(report);
    setActionDialog(action);
  };

  return (
    <div className="max-w-7xl mx-auto p-4 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)] flex items-center gap-3">
            <Flag className="w-8 h-8 text-[var(--color-warning)]" />
            Content Reports
          </h1>
          <p className="text-[var(--color-text-secondary)]">Review and moderate reported content</p>
        </div>
      </div>

      {/* Filters */}
      <Card className="p-4">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1 min-w-[250px]">
            <Input
              placeholder="Search reports..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              leftIcon={<Search className="w-4 h-4" />}
            />
          </div>
          <Select value={statusFilter} onValueChange={setStatusFilter} className="w-48">
            <Select.Trigger><Select.Value placeholder="All Status" /></Select.Trigger>
            <Select.Content>
              <Select.Item value="all">All Status</Select.Item>
              <Select.Item value="PENDING">Pending</Select.Item>
              <Select.Item value="UNDER_REVIEW">Under Review</Select.Item>
              <Select.Item value="RESOLVED">Resolved</Select.Item>
              <Select.Item value="DISMISSED">Dismissed</Select.Item>
              <Select.Item value="ESCALATED">Escalated</Select.Item>
            </Select.Content>
          </Select>
          <Select value={typeFilter} onValueChange={setTypeFilter} className="w-48">
            <Select.Trigger><Select.Value placeholder="All Types" /></Select.Trigger>
            <Select.Content>
              <Select.Item value="all">All Types</Select.Item>
              <Select.Item value="VIDEO">Video</Select.Item>
              <Select.Item value="COMMENT">Comment</Select.Item>
              <Select.Item value="CHANNEL">Channel</Select.Item>
              <Select.Item value="USER">User</Select.Item>
              <Select.Item value="PLAYLIST">Playlist</Select.Item>
            </Select.Content>
          </Select>
        </div>
      </Card>

      {/* Reports Table */}
      <Card>
        {isLoading ? (
          <div className="p-8 space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="animate-pulse flex items-center gap-4">
                <div className="w-10 h-10 rounded-lg bg-[var(--color-bg-tertiary)]" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-1/3 bg-[var(--color-bg-tertiary)] rounded" />
                  <div className="h-3 w-1/4 bg-[var(--color-bg-tertiary)] rounded" />
                </div>
                <div className="w-32 h-8 bg-[var(--color-bg-tertiary)] rounded" />
              </div>
            ))}
          </div>
        ) : reports?.content?.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-[var(--color-border)] text-left text-sm font-medium text-[var(--color-text-secondary)]">
                    <th className="pb-3 pr-4">Report</th>
                    <th className="pb-3 pr-4">Target</th>
                    <th className="pb-3 pr-4">Reason</th>
                    <th className="pb-3 pr-4">Reporter</th>
                    <th className="pb-3 pr-4">Status</th>
                    <th className="pb-3 pr-4">Created</th>
                    <th className="pb-3 pr-4">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {reports.content.map((report: any) => (
                    <tr key={report.id} className="border-b border-[var(--color-border)]/50 hover:bg-[var(--color-bg-hover)]/50">
                      <td className="py-4 pr-4">
                        <div>
                          <p className="font-medium text-[var(--color-text-primary)] truncate max-w-xs">{report.description || 'No description'}</p>
                          <p className="text-xs text-[var(--color-text-secondary)]">ID: {report.id.slice(0, 8)}...</p>
                        </div>
                      </td>
                      <td className="py-4 pr-4">
                        <div>
                          <p className="font-medium text-[var(--color-text-primary)]">{report.targetTitle || 'Unknown'}</p>
                          <p className="text-xs text-[var(--color-text-secondary)]">
                            {report.targetType} • {report.targetId?.slice(0, 8)}...
                          </p>
                        </div>
                      </td>
                      <td className="py-4 pr-4">
                        <Badge variant={
                          report.reason === 'SPAM' ? 'warning' :
                          report.reason === 'HATE_SPEECH' ? 'error' :
                          report.reason === 'SEXUAL_CONTENT' ? 'error' :
                          report.reason === 'VIOLENCE' ? 'error' :
                          report.reason === 'CHILD_SAFETY' ? 'error' :
                          report.reason === 'COPYRIGHT' ? 'info' :
                          'default'
                        }>
                          {report.reason}
                        </Badge>
                      </td>
                      <td className="py-4 pr-4">
                        <div>
                          <p className="font-medium text-[var(--color-text-primary)]">{report.reporterName || 'Unknown'}</p>
                          <p className="text-xs text-[var(--color-text-secondary)]">@{report.reporterUsername}</p>
                        </div>
                      </td>
                      <td className="py-4 pr-4">
                        <Badge variant={
                          report.status === 'PENDING' ? 'warning' :
                          report.status === 'UNDER_REVIEW' ? 'info' :
                          report.status === 'RESOLVED' ? 'success' :
                          report.status === 'DISMISSED' ? 'default' :
                          report.status === 'ESCALATED' ? 'error' :
                          'default'
                        }>
                          {report.status}
                        </Badge>
                      </td>
                      <td className="py-4 pr-4 text-[var(--color-text-secondary)] text-sm">
                        {formatDistanceToNow(new Date(report.createdAt), { addSuffix: true })}
                      </td>
                      <td className="py-4 pr-4">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => handleAction(report.status === 'RESOLVED' || report.status === 'DISMISSED' ? 'dismiss' : 'resolve', report)}
                            className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors text-[var(--color-text-secondary)] hover:text-[var(--color-success)]"
                            title={report.status === 'RESOLVED' ? 'Mark Resolved' : 'Resolve'}
                          >
                            <Check className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleAction('dismiss', report)}
                            className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors text-[var(--color-text-secondary)] hover:text-[var(--color-text-secondary)]"
                            title="Dismiss"
                          >
                            <X className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleAction('escalate', report)}
                            className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors text-[var(--color-text-secondary)] hover:text-[var(--color-warning)]"
                            title="Escalate"
                          >
                            <AlertTriangle className="w-4 h-4" />
                          </button>
                          <Link to={`/admin/reports/${report.id}`} className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors text-[var(--color-text-secondary)] hover:text-[var(--color-primary)]" title="View Details">
                            <MessageCircle className="w-4 h-4" />
                          </Link>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {reports.totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 p-4 border-t border-[var(--color-border)]">
                <Button variant="outline" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                  Previous
                </Button>
                <span className="px-4 text-[var(--color-text-secondary)]">
                  Page {page + 1} of {reports.totalPages}
                </span>
                <Button variant="outline" disabled={page >= reports.totalPages - 1} onClick={() => setPage(p => p + 1)}>
                  Next
                </Button>
              </div>
            )}
          </>
        ) : (
          <div className="p-8 text-center">
            <Flag className="w-16 h-16 mx-auto text-[var(--color-text-tertiary)] mb-4 opacity-50" />
            <h3 className="text-lg font-medium text-[var(--color-text-primary)] mb-2">No reports found</h3>
            <p className="text-[var(--color-text-secondary)]">Try adjusting your search or filters</p>
          </div>
        )}
      </Card>

      {/* Action Dialogs */}
      {actionDialog === 'resolve' && selectedReport && (
        <ResolveDialog report={selectedReport} onClose={() => { setActionDialog(null); setSelectedReport(null); }} onConfirm={(action, notes) => resolveMutation.mutate({ reportId: selectedReport.id, action, notes })} />
      )}
      {actionDialog === 'dismiss' && selectedReport && (
        <ConfirmDialog
          title="Dismiss Report"
          message={`Are you sure you want to dismiss this report? The content will not be reviewed.`}
          onClose={() => { setActionDialog(null); setSelectedReport(null); }}
          onConfirm={() => resolveMutation.mutate({ reportId: selectedReport.id, action: 'DISMISSED', notes: 'Dismissed by moderator' })}
        />
      )}
      {actionDialog === 'escalate' && selectedReport && (
        <EscalateDialog report={selectedReport} onClose={() => { setActionDialog(null); setSelectedReport(null); }} onConfirm={(notes) => resolveMutation.mutate({ reportId: selectedReport.id, action: 'ESCALATED', notes })} />
      )}
    </div>
  );
}

function ResolveDialog({ report, onClose, onConfirm }: { report: any; onClose: () => void; onConfirm: (action: string, notes: string) => void }) {
  const [action, setAction] = React.useState<'APPROVE' | 'REJECT'>('APPROVE');
  const [notes, setNotes] = React.useState('');
  return (
    <Dialog open onOpenChange={onClose}>
      <Dialog.Content>
        <Dialog.Header>
          <Dialog.Title>Review Report</Dialog.Title>
          <Dialog.Description>Review and take action on this report</Dialog.Description>
        </Dialog.Header>
        <form onSubmit={(e) => { e.preventDefault(); onConfirm(action, notes); onClose(); }} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-2">Action</label>
            <Select value={action} onValueChange={setAction}>
              <Select.Trigger><Select.Value /></Select.Trigger>
              <Select.Content>
                <Select.Item value="APPROVE">Approve - Take action on content</Select.Item>
                <Select.Item value="REJECT">Reject - Content is fine, dismiss report</Select.Item>
              </Select.Content>
            </Select>
          </div>
          <Input label="Notes" value={notes} onChange={(e) => setNotes(e.target.value)} as="textarea" rows={3} placeholder="Optional notes for the decision" />
          <Dialog.Footer>
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" variant={action === 'REJECT' ? 'outline' : 'danger'}>
              {action === 'REJECT' ? 'Dismiss Report' : 'Take Action'}
            </Button>
          </Dialog.Footer>
        </form>
      </Dialog.Content>
    </Dialog>
  );
}

function EscalateDialog({ report, onClose, onConfirm }: { report: any; onClose: () => void; onConfirm: (notes: string) => void }) {
  const [notes, setNotes] = React.useState('');
  return (
    <Dialog open onOpenChange={onClose}>
      <Dialog.Content>
        <Dialog.Header>
          <Dialog.Title>Escalate Report</Dialog.Title>
          <Dialog.Description>Escalate this report to senior moderation team</Dialog.Description>
        </Dialog.Header>
        <form onSubmit={(e) => { e.preventDefault(); onConfirm(notes); onClose(); }} className="space-y-4">
          <Input label="Escalation Notes" value={notes} onChange={(e) => setNotes(e.target.value)} as="textarea" rows={3} required placeholder="Why should this be escalated?" />
          <Dialog.Footer>
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" variant="warning">Escalate</Button>
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