-- Approved target: rasudbiaxwyoojxxdutz. Review before applying to another environment.
-- Restricts writes only; existing public image URLs and stored objects are preserved.
BEGIN;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM storage.buckets WHERE id='productos' AND public=true) THEN
    RAISE EXCEPTION 'Expected public productos bucket was not found; no changes applied';
  END IF;
  IF (SELECT count(*) FROM pg_policies WHERE schemaname='storage' AND tablename='objects'
      AND cmd IN ('INSERT','UPDATE','DELETE','ALL')) <> 3
    OR (SELECT count(*) FROM pg_policies WHERE schemaname='storage' AND tablename='objects'
      AND policyname IN ('Public Insert Productos','Public Update Productos','Public Delete Productos')
      AND roles=ARRAY['public']::name[]) <> 3 THEN
    RAISE EXCEPTION 'Storage write policies differ from the reviewed snapshot; inspect before applying';
  END IF;
END $$;

DROP POLICY "Public Insert Productos" ON storage.objects;
DROP POLICY "Public Update Productos" ON storage.objects;
DROP POLICY "Public Delete Productos" ON storage.objects;

CREATE POLICY productos_admin_insert ON storage.objects
  FOR INSERT TO authenticated
  WITH CHECK (bucket_id='productos' AND (SELECT auth.jwt()->'app_metadata'->>'role')='admin');

CREATE POLICY productos_admin_update ON storage.objects
  FOR UPDATE TO authenticated
  USING (bucket_id='productos' AND (SELECT auth.jwt()->'app_metadata'->>'role')='admin')
  WITH CHECK (bucket_id='productos' AND (SELECT auth.jwt()->'app_metadata'->>'role')='admin');

CREATE POLICY productos_admin_delete ON storage.objects
  FOR DELETE TO authenticated
  USING (bucket_id='productos' AND (SELECT auth.jwt()->'app_metadata'->>'role')='admin');

-- Bucket configuration only. No rows in storage.objects or file bytes are modified.
UPDATE storage.buckets SET file_size_limit=8388608,
  allowed_mime_types=ARRAY['image/jpeg','image/png','image/webp','image/avif']::text[]
  WHERE id='productos';
COMMIT;
