-- Notes full sync schema.
-- Run this in Supabase SQL Editor for the project used by the app.
-- The app writes to public.user_notes, public.note_images,
-- public.user_note_attachments, and Storage bucket note-attachments.

create extension if not exists pgcrypto;

create table if not exists public.user_notes (
  id uuid not null default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  content text not null,
  created_at timestamp with time zone not null default timezone('utc'::text, now()),
  updated_at timestamp with time zone not null default timezone('utc'::text, now()),
  deleted_at timestamp with time zone null,
  constraint user_notes_pkey primary key (id)
);

alter table public.user_notes
  add column if not exists title text null,
  add column if not exists has_reminder boolean not null default false,
  add column if not exists has_reminder_badge boolean not null default false;

create table if not exists public.note_images (
  id uuid not null default gen_random_uuid(),
  note_id uuid not null references public.user_notes(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  path text not null,
  mime_type text null,
  width integer null,
  height integer null,
  created_at timestamp with time zone not null default timezone('utc'::text, now()),
  constraint note_images_pkey primary key (id)
);

alter table public.note_images
  add column if not exists mime_type text null,
  add column if not exists width integer null,
  add column if not exists height integer null,
  add column if not exists created_at timestamp with time zone not null default timezone('utc'::text, now());

create index if not exists note_images_note_id_idx
  on public.note_images using btree(note_id);

create table if not exists public.user_note_attachments (
  id uuid not null default gen_random_uuid(),
  note_id uuid not null references public.user_notes(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  storage_path text not null,
  file_name text not null,
  mime_type text null,
  size_bytes bigint not null default 0,
  kind text not null check (kind in ('file', 'audio', 'video', 'voice')),
  duration_ms bigint null,
  created_at_ms bigint null,
  created_at timestamp with time zone not null default timezone('utc'::text, now()),
  constraint user_note_attachments_pkey primary key (id)
);

alter table public.user_note_attachments
  add column if not exists mime_type text null,
  add column if not exists size_bytes bigint not null default 0,
  add column if not exists kind text not null default 'file',
  add column if not exists duration_ms bigint null,
  add column if not exists created_at_ms bigint null,
  add column if not exists created_at timestamp with time zone not null default timezone('utc'::text, now());

create index if not exists user_note_attachments_note_id_idx
  on public.user_note_attachments using btree(note_id);

create or replace function public.set_updated_at_user_notes()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = timezone('utc'::text, now());
  return new;
end;
$$;

drop trigger if exists trg_update_notes_updated_at on public.user_notes;
create trigger trg_update_notes_updated_at
before update on public.user_notes
for each row
execute function public.set_updated_at_user_notes();

alter table public.user_notes enable row level security;
alter table public.note_images enable row level security;
alter table public.user_note_attachments enable row level security;

grant select, insert, update, delete on public.user_notes to authenticated;
grant select, insert, update, delete on public.note_images to authenticated;
grant select, insert, update, delete on public.user_note_attachments to authenticated;

drop policy if exists "read own notes" on public.user_notes;
drop policy if exists "insert own notes" on public.user_notes;
drop policy if exists "update own notes" on public.user_notes;
drop policy if exists "delete own notes" on public.user_notes;

create policy "read own notes"
on public.user_notes for select
to authenticated
using (auth.uid() = user_id);

create policy "insert own notes"
on public.user_notes for insert
to authenticated
with check (auth.uid() = user_id);

create policy "update own notes"
on public.user_notes for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "delete own notes"
on public.user_notes for delete
to authenticated
using (auth.uid() = user_id);

drop policy if exists "read own note images" on public.note_images;
drop policy if exists "insert own note images" on public.note_images;
drop policy if exists "update own note images" on public.note_images;
drop policy if exists "delete own note images" on public.note_images;

create policy "read own note images"
on public.note_images for select
to authenticated
using (auth.uid() = user_id);

create policy "insert own note images"
on public.note_images for insert
to authenticated
with check (auth.uid() = user_id);

create policy "update own note images"
on public.note_images for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "delete own note images"
on public.note_images for delete
to authenticated
using (auth.uid() = user_id);

drop policy if exists "read own note attachments" on public.user_note_attachments;
drop policy if exists "insert own note attachments" on public.user_note_attachments;
drop policy if exists "update own note attachments" on public.user_note_attachments;
drop policy if exists "delete own note attachments" on public.user_note_attachments;

create policy "read own note attachments"
on public.user_note_attachments for select
to authenticated
using (auth.uid() = user_id);

create policy "insert own note attachments"
on public.user_note_attachments for insert
to authenticated
with check (auth.uid() = user_id);

create policy "update own note attachments"
on public.user_note_attachments for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "delete own note attachments"
on public.user_note_attachments for delete
to authenticated
using (auth.uid() = user_id);

insert into storage.buckets (id, name, public)
values ('note-attachments', 'note-attachments', false)
on conflict (id) do nothing;

drop policy if exists "read own note storage" on storage.objects;
drop policy if exists "insert own note storage" on storage.objects;
drop policy if exists "update own note storage" on storage.objects;
drop policy if exists "delete own note storage" on storage.objects;

create policy "read own note storage"
on storage.objects for select
to authenticated
using (
  bucket_id = 'note-attachments'
  and (storage.foldername(name))[1] = 'notes'
  and (storage.foldername(name))[2] = auth.uid()::text
);

create policy "insert own note storage"
on storage.objects for insert
to authenticated
with check (
  bucket_id = 'note-attachments'
  and (storage.foldername(name))[1] = 'notes'
  and (storage.foldername(name))[2] = auth.uid()::text
);

create policy "update own note storage"
on storage.objects for update
to authenticated
using (
  bucket_id = 'note-attachments'
  and (storage.foldername(name))[1] = 'notes'
  and (storage.foldername(name))[2] = auth.uid()::text
)
with check (
  bucket_id = 'note-attachments'
  and (storage.foldername(name))[1] = 'notes'
  and (storage.foldername(name))[2] = auth.uid()::text
);

create policy "delete own note storage"
on storage.objects for delete
to authenticated
using (
  bucket_id = 'note-attachments'
  and (storage.foldername(name))[1] = 'notes'
  and (storage.foldername(name))[2] = auth.uid()::text
);
